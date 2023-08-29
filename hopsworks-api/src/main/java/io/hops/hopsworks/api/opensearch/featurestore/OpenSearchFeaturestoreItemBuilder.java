/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.api.opensearch.featurestore;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.hops.hopsworks.api.user.UserDTO;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.featurestore.xattr.dto.FeatureStoreItem;
import io.hops.hopsworks.common.featurestore.xattr.dto.FeatureViewXAttrDTO;
import io.hops.hopsworks.common.featurestore.xattr.dto.FeaturegroupXAttr;
import io.hops.hopsworks.common.opensearch.OpenSearchFeaturestoreHit;
import io.hops.hopsworks.common.featurestore.xattr.dto.FeaturestoreXAttrsConstants;
import io.hops.hopsworks.common.featurestore.xattr.dto.TrainingDatasetXAttrDTO;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.Date;
import java.util.logging.Level;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class OpenSearchFeaturestoreItemBuilder {

  @EJB
  private UserFacade userFacade;
  
  public OpenSearchFeaturestoreItemBuilder() {}
  
  // For testing
  protected OpenSearchFeaturestoreItemBuilder(UserFacade userFacade) {
    this.userFacade = userFacade;
  }

  public OpenSearchFeaturestoreItemDTO.Base fromBaseArtifact(OpenSearchFeaturestoreHit hit) throws GenericException {
    OpenSearchFeaturestoreItemDTO.Base item = new OpenSearchFeaturestoreItemDTO.Base();
    item.elasticId = hit.getId();
    item.name = hit.getName();
    item.version = hit.getVersion();
    item.datasetIId = hit.getDatasetIId();
    item.parentProjectId = hit.getProjectId();
    item.parentProjectName = hit.getProjectName();
    Object featureStoreXAttr = hit.getXattrs().get(FeaturestoreXAttrsConstants.FEATURESTORE);
    if(featureStoreXAttr != null) {
      switch(hit.getDocType()) {
        case "featuregroup": {
          populateFeatureStoreItem(item, parseAttr(featureStoreXAttr, FeaturegroupXAttr.FullDTO.class));
        } break;
        case "featureview": {
          populateFeatureStoreItem(item, parseAttr(featureStoreXAttr, FeatureViewXAttrDTO.class));
        } break;
        case "trainingdataset": {
          populateFeatureStoreItem(item, parseAttr(featureStoreXAttr, TrainingDatasetXAttrDTO.class));
        } break;
        default: break;
      }
    }
    return item;
  }
  
  private OpenSearchFeaturestoreItemDTO.Base populateFeatureStoreItem(OpenSearchFeaturestoreItemDTO.Base item,
                                                                      FeatureStoreItem src){
    item.featurestoreId = src.getFeaturestoreId();
    item.description = src.getDescription();
    item.created = new Date(src.getCreateDate());
    item.creator = new UserDTO(userFacade.findByEmail(src.getCreator()));
    return item;
  }
  
  private <C> C parseAttr(Object value, Class<C> type) throws GenericException {
    ObjectMapper objectMapper = new ObjectMapper();
    try {
      return objectMapper.convertValue(value, type);
    } catch (IllegalArgumentException ex) {
      String errMsg = "search failed due to document parsing issues";
      throw new GenericException(RESTCodes.GenericErrorCode.UNKNOWN_ERROR, Level.WARNING, errMsg, errMsg, ex);
    }
  }
  
  public OpenSearchFeaturestoreItemDTO.Feature fromFeature(String featureName,
                                                           String featureDescription,
                                                           OpenSearchFeaturestoreItemDTO.Base parent) {
    OpenSearchFeaturestoreItemDTO.Feature item = new OpenSearchFeaturestoreItemDTO.Feature();
    item.elasticId = parent.getElasticId() + "_" + featureName;
    item.featurestoreId = parent.getFeaturestoreId();
    item.name = featureName;
    item.description = featureDescription;
    item.featuregroup = parent.getName();
    item.datasetIId = parent.getDatasetIId();
    item.version = parent.getVersion();
    item.created = parent.getCreated();
    item.creator = parent.getCreator();

    item.parentProjectId = parent.getParentProjectId();
    item.parentProjectName = parent.getParentProjectName();
    return item;
  }
}
