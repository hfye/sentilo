/*
 * Sentilo
 * 
 * Copyright (C) 2013 Institut Municipal d’Informàtica, Ajuntament de Barcelona.
 * 
 * This program is licensed and may be used, modified and redistributed under the terms of the
 * European Public License (EUPL), either version 1.1 or (at your option) any later version as soon
 * as they are approved by the European Commission.
 * 
 * Alternatively, you may redistribute and/or modify this program under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.
 * 
 * See the licenses for the specific language governing permissions, limitations and more details.
 * 
 * You should have received a copy of the EUPL1.1 and the LGPLv3 licenses along with this program;
 * if not, you may find them at:
 * 
 * https://joinup.ec.europa.eu/software/page/eupl/licence-eupl http://www.gnu.org/licenses/ and
 * https://www.gnu.org/licenses/lgpl.txt
 */
package org.sentilo.platform.server.handler.impl;

import java.util.List;

import org.sentilo.common.domain.CatalogAlert;
import org.sentilo.common.domain.CatalogAlertInputMessage;
import org.sentilo.common.domain.CatalogResponseMessage;
import org.sentilo.platform.common.exception.PlatformException;
import org.sentilo.platform.common.service.CatalogService;
import org.sentilo.platform.server.exception.CatalogErrorException;
import org.sentilo.platform.server.exception.ForbiddenAccessException;
import org.sentilo.platform.server.handler.AbstractHandler;
import org.sentilo.platform.server.parser.CatalogAlertParser;
import org.sentilo.platform.server.request.SentiloRequest;
import org.sentilo.platform.server.response.SentiloResponse;
import org.sentilo.platform.server.validation.CatalogAlertValidator;
import org.sentilo.platform.server.validation.RequestMessageValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;

import com.google.common.base.Function;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

@Controller
public class CatalogAlertHandler extends AbstractHandler {

  private final Logger logger = LoggerFactory.getLogger(CatalogAlertHandler.class);

  @Autowired
  private CatalogService catalogService;

  private final CatalogAlertParser parser = new CatalogAlertParser();
  private final RequestMessageValidator<CatalogAlertInputMessage> validator = new CatalogAlertValidator();

  @Override
  public void onDelete(final SentiloRequest request, final SentiloResponse response) throws PlatformException {
    logger.debug("Executing catalog alert DELETE request");
    debug(request);

    _onDelete(request, response, false);
  }

  @Override
  public void onGet(final SentiloRequest request, final SentiloResponse response) throws PlatformException {
    logger.debug("Executing catalog alert GET request");
    debug(request);

    // La peticion sólo puede ser de la siguiente manera
    // GET /catalog/alert/<entity>
    // Ademas, puede haber parametros en la URL con los cuales filtrar las alertas a retornar

    validateResourceNumberParts(request, 0, 1);
    final CatalogAlertInputMessage inputMessage = parser.parseGetRequest(request);
    validator.validateRequestMessageOnGet(inputMessage);
    validateEntityAndTokenAreEquals(inputMessage, request);

    // Aqui no tiene sentido hacer ninguna validación de autorizacion ya que se aplica sobre la
    // misma entidad que hace la peticion
    final CatalogResponseMessage responseMessage = catalogService.getAuthorizedAlerts(inputMessage);
    if (!responseMessage.getCode().equals(CatalogResponseMessage.OK)) {
      throw new CatalogErrorException(responseMessage.getCode(), responseMessage.getErrorMessage());
    }

    parser.writeResponse(request, response, responseMessage);
  }

  @Override
  public void onPost(final SentiloRequest request, final SentiloResponse response) throws PlatformException {
    logger.debug("Executing catalog alert POST request");
    debug(request);

    // La peticion sólo puede ser de la siguiente manera
    // POST /catalog/alert/<entity>

    validateResourceNumberParts(request, 0, 1);
    final CatalogAlertInputMessage inputMessage = parser.parsePostRequest(request);
    validator.validateRequestMessageOnPost(inputMessage);
    validateEntityAndTokenAreEquals(inputMessage, request);
    validateAuthorization(inputMessage, request);

    // Redirigir peticion al catálogo --> Cliente REST para el catálogo.
    final CatalogResponseMessage responseMessage = catalogService.insertAlerts(inputMessage);
    if (!responseMessage.getCode().equals(CatalogResponseMessage.OK)) {
      throw new CatalogErrorException(responseMessage.getCode(), responseMessage.getErrorMessage());
    }
  }

  @Override
  public void onPut(final SentiloRequest request, final SentiloResponse response) throws PlatformException {
    logger.debug("Executing catalog alert PUT request");
    debug(request);

    final String method = request.getRequestParameter("method");
    if (StringUtils.hasText(method) && method.equals("delete")) {
      _onDelete(request, response, true);
    } else {
      _onPut(request, response);
    }

  }

  private void _onDelete(final SentiloRequest request, final SentiloResponse response, final boolean simulate) throws PlatformException {
    logger.debug("Executing catalog alert DELETE request");
    debug(request);

    // La peticion sólo puede ser de la sigiente manera
    // DELETE /catalog/alert/<entity>

    validateResourceNumberParts(request, 0, 1);
    final CatalogAlertInputMessage inputMessage = parser.parseDeleteRequest(request, simulate);
    validator.validateRequestMessageOnDelete(inputMessage);
    validateEntityAndTokenAreEquals(inputMessage, request);

    final CatalogResponseMessage responseMessage = catalogService.deleteAlerts(inputMessage);
    if (!responseMessage.getCode().equals(CatalogResponseMessage.OK)) {
      throw new CatalogErrorException(responseMessage.getCode(), responseMessage.getErrorMessage());
    }

  }

  public void _onPut(final SentiloRequest request, final SentiloResponse response) throws PlatformException {
    logger.debug("Executing catalog alert PUT request");
    debug(request);

    // La peticion sólo puede ser de la sigiente manera
    // PUT /catalog/alert/<entity>

    validateResourceNumberParts(request, 0, 1);
    final CatalogAlertInputMessage inputMessage = parser.parsePutRequest(request);
    validator.validateRequestMessageOnPut(inputMessage);
    validateEntityAndTokenAreEquals(inputMessage, request);
    validateAuthorization(inputMessage, request);

    final CatalogResponseMessage responseMessage = catalogService.updateAlerts(inputMessage);
    if (!responseMessage.getCode().equals(CatalogResponseMessage.OK)) {
      throw new CatalogErrorException(responseMessage.getCode(), responseMessage.getErrorMessage());
    }
  }

  protected void validateEntityAndTokenAreEquals(final CatalogAlertInputMessage inputMessage, final SentiloRequest request)
      throws ForbiddenAccessException {
    // If token entity not correspond to catalog app then token entity and resource entity must be
    // equals
    if (!getCatalogId().equals(request.getEntitySource()) && !inputMessage.getEntityId().equals(request.getEntitySource())) {
      final String errorMessage = String.format("You are not %s entity. Review token identity or request path content.", inputMessage.getEntityId());
      throw new ForbiddenAccessException(errorMessage);
    }
  }

  protected void validateAuthorization(final CatalogAlertInputMessage inputMessage, final SentiloRequest request) throws ForbiddenAccessException {
    // Internal alerts only could be inserted/updated by catalog entity
    Multimap<String, CatalogAlert> groups = groupAlertsByType(inputMessage.getAlerts());
    if (groups.get("INTERNAL").size() > 0 && !getCatalogId().equals(request.getEntitySource())) {
      final String errorMessage = String.format("You are not authorized to insert/update internal alerts.");
      throw new ForbiddenAccessException(errorMessage);
    }
  }

  /**
   * Group alerts list by type (INTERNAL or EXTERNAL)
   * 
   * @param alerts
   * @return
   */
  protected Multimap<String, CatalogAlert> groupAlertsByType(List<CatalogAlert> alerts) {
    Function<CatalogAlert, String> internalFunction = new Function<CatalogAlert, String>() {
      @Override
      public String apply(final CatalogAlert alert) {
        // alert type could be null if it is wrong, so return empty string if it is null
        return (alert.getType() != null ? alert.getType() : "");
      }
    };
    return Multimaps.index(alerts, internalFunction);
  }
}