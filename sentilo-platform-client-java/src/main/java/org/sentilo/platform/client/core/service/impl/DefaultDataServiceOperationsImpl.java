/*
 * Sentilo
 *  
 * Original version 1.4 Copyright (C) 2013 Institut Municipal d’Informàtica, Ajuntament de Barcelona.
 * Modified by Opentrends adding support for multitenant deployments and SaaS. Modifications on version 1.5 Copyright (C) 2015 Opentrends Solucions i Sistemes, S.L.
 * 
 *   
 * This program is licensed and may be used, modified and redistributed under the
 * terms  of the European Public License (EUPL), either version 1.1 or (at your 
 * option) any later version as soon as they are approved by the European 
 * Commission.
 *   
 * Alternatively, you may redistribute and/or modify this program under the terms
 * of the GNU Lesser General Public License as published by the Free Software 
 * Foundation; either  version 3 of the License, or (at your option) any later 
 * version. 
 *   
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR 
 * CONDITIONS OF ANY KIND, either express or implied. 
 *   
 * See the licenses for the specific language governing permissions, limitations 
 * and more details.
 *   
 * You should have received a copy of the EUPL1.1 and the LGPLv3 licenses along 
 * with this program; if not, you may find them at: 
 *   
 *   https://joinup.ec.europa.eu/software/page/eupl/licence-eupl
 *   http://www.gnu.org/licenses/ 
 *   and 
 *   https://www.gnu.org/licenses/lgpl.txt
 */
package org.sentilo.platform.client.core.service.impl;

import org.sentilo.common.converter.StringMessageConverter;
import org.sentilo.platform.client.core.domain.DataInputMessage;
import org.sentilo.platform.client.core.domain.ObservationsOutputMessage;
import org.sentilo.platform.client.core.parser.DataMessageConverter;
import org.sentilo.platform.client.core.service.DataServiceOperations;
import org.sentilo.platform.client.core.utils.RequestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DefaultDataServiceOperationsImpl extends AbstractServiceOperationsImpl implements DataServiceOperations {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultDataServiceOperationsImpl.class);

  private StringMessageConverter converter = new DataMessageConverter();

  @Override
  public ObservationsOutputMessage getLastObservations(final DataInputMessage message) {
    LOGGER.debug("Retrieving last observations  {}", message);
    final String response = getRestClient().get(RequestUtils.buildPath(message), RequestUtils.buildParameters(message), message.getIdentityToken());
    LOGGER.debug("Retrieved last observations");
    return (ObservationsOutputMessage) converter.unmarshal(response, ObservationsOutputMessage.class);
  }

  @Override
  public void removeLastObservations(final DataInputMessage message) {
    LOGGER.debug("Removing last observations  {}", message);
    getRestClient().delete(RequestUtils.buildPath(message), message.getIdentityToken());
    LOGGER.debug("Removed last observations");
  }

  @Override
  public void sendObservations(final DataInputMessage message) {
    // Tenemos 3 opciones a la hora de hacer la llamada en función de como esté/esten informadas
    // las observaciones. Pero todas ellas solo afectan al contenido del body.
    LOGGER.debug("Sending observations  {}", message);
    getRestClient().put(RequestUtils.buildPath(message), converter.marshal(message), message.getIdentityToken());
    LOGGER.debug("Observations has been sent");
  }

}
