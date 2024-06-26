/*
 * This file is part of Hopsworks
 * Copyright (C) 2022, Hopsworks AB. All rights reserved
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
package io.hops.hopsworks.api.provenance.explicit;

import io.hops.hopsworks.api.featurestore.featureview.FeatureViewBuilder;
import io.hops.hopsworks.api.featurestore.trainingdataset.TrainingDatasetDTOBuilder;
import io.hops.hopsworks.api.modelregistry.models.ModelsBuilder;
import io.hops.hopsworks.api.modelregistry.models.dto.ModelDTO;
import io.hops.hopsworks.api.provenance.explicit.dto.ProvArtifactDTO;
import io.hops.hopsworks.api.provenance.explicit.dto.featurestore.ProvCachedFeatureGroupDTO;
import io.hops.hopsworks.api.provenance.explicit.dto.featurestore.ProvOnDemandFeatureGroupDTO;
import io.hops.hopsworks.api.provenance.explicit.dto.featurestore.ProvStreamFeatureGroupDTO;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.api.RestDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupController;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.cached.CachedFeaturegroupDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.ondemand.OnDemandFeaturegroupDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.stream.StreamFeatureGroupDTO;
import io.hops.hopsworks.common.featurestore.featureview.FeatureViewDTO;
import io.hops.hopsworks.common.featurestore.storageconnectors.FeaturestoreStorageConnectorController;
import io.hops.hopsworks.common.featurestore.storageconnectors.FeaturestoreStorageConnectorDTO;
import io.hops.hopsworks.common.featurestore.trainingdatasets.TrainingDatasetDTO;
import io.hops.hopsworks.common.featurestore.utils.FeaturestoreUtils;
import io.hops.hopsworks.common.provenance.explicit.ProvArtifact;
import io.hops.hopsworks.common.provenance.explicit.ProvExplicitLink;
import io.hops.hopsworks.api.provenance.explicit.dto.ProvNodeDTO;
import io.hops.hopsworks.api.provenance.explicit.dto.ProvExplicitLinkDTO;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.FeatureStoreMetadataException;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.exceptions.MetadataException;
import io.hops.hopsworks.exceptions.ModelRegistryException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featureview.FeatureView;
import io.hops.hopsworks.persistence.entity.featurestore.storageconnector.FeaturestoreConnector;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;
import io.hops.hopsworks.persistence.entity.models.version.ModelVersion;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;
import io.hops.hopsworks.restutils.RESTException;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ProvExplicitLinksBuilder {
  @EJB
  private FeaturestoreUtils featurestoreUtils;
  @EJB
  private FeaturestoreStorageConnectorController featurestoreStorageConnectorController;
  @EJB
  private FeaturegroupController featuregroupController;
  @EJB
  private FeatureViewBuilder featureViewBuilder;
  @EJB
  private TrainingDatasetDTOBuilder trainingDatasetBuilder;
  @EJB
  private ModelsBuilder modelsBuilder;

  public ProvExplicitLinksBuilder() {}
  
  //test
  public ProvExplicitLinksBuilder(FeaturestoreUtils featurestoreUtils,
                                  FeaturestoreStorageConnectorController featurestoreStorageConnectorController,
                                  FeaturegroupController featuregroupController,
                                  FeatureViewBuilder featureViewBuilder,
                                  TrainingDatasetDTOBuilder trainingDatasetBuilder) {
    this.featurestoreUtils = featurestoreUtils;
    this.featurestoreStorageConnectorController = featurestoreStorageConnectorController;
    this.featuregroupController = featuregroupController;
    this.featureViewBuilder = featureViewBuilder;
    this.trainingDatasetBuilder = trainingDatasetBuilder;
  }

  public UriBuilder storageConnectorURI(UriInfo uriInfo, Project accessProject, FeaturestoreConnector connector) {
    return featurestoreUtils.storageConnectorByNameURI(uriInfo.getBaseUriBuilder(), accessProject, connector)
      .path(ResourceRequest.Name.PROVENANCE.toString().toLowerCase())
      .path(ResourceRequest.Name.LINKS.toString().toLowerCase());
  }
  
  public UriBuilder featureGroupURI(UriInfo uriInfo, Project accessProject, Featuregroup featureGroup) {
    return featurestoreUtils.featureGroupByIdURI(uriInfo.getBaseUriBuilder(), accessProject, featureGroup)
      .path(ResourceRequest.Name.PROVENANCE.toString().toLowerCase())
      .path(ResourceRequest.Name.LINKS.toString().toLowerCase());
  }
  
  public UriBuilder featureViewURI(UriInfo uriInfo, Project accessProject, FeatureView featureView) {
    return featurestoreUtils.featureViewURI(uriInfo.getBaseUriBuilder(), accessProject, featureView)
      .path(ResourceRequest.Name.PROVENANCE.toString().toLowerCase())
      .path(ResourceRequest.Name.LINKS.toString().toLowerCase());
  }
  
  public UriBuilder trainingDatasetURI(UriInfo uriInfo, Project accessProject, TrainingDataset trainingDataset) {
    return featurestoreUtils.trainingDatasetURI(uriInfo.getBaseUriBuilder(), accessProject, trainingDataset)
      .path(ResourceRequest.Name.PROVENANCE.toString().toLowerCase())
      .path(ResourceRequest.Name.LINKS.toString().toLowerCase());
  }

  public UriBuilder modelURI(UriInfo uriInfo, Project accessProject, ModelVersion model) {
    return modelsBuilder.modelVersionUri(uriInfo, accessProject, model.getModel().getProject(),  model)
        .path(ResourceRequest.Name.PROVENANCE.toString().toLowerCase())
        .path(ResourceRequest.Name.LINKS.toString().toLowerCase());
  }
  
  public ProvExplicitLinkDTO<FeaturegroupDTO> buildFGLinks(UriInfo uriInfo, ResourceRequest resourceRequest,
                                      Project accessProject, Users user, ProvExplicitLink<Featuregroup> links)
      throws DatasetException, FeatureStoreMetadataException, MetadataException, ServiceException,
      GenericException, FeaturestoreException, IOException, ModelRegistryException {
    return (ProvExplicitLinkDTO<FeaturegroupDTO>)build(uriInfo, resourceRequest, accessProject, user, links);
  }
  
  public ProvExplicitLinkDTO<FeatureViewDTO> buildFVLinks(UriInfo uriInfo, ResourceRequest resourceRequest,
                                                          Project accessProject, Users user,
                                                          ProvExplicitLink<FeatureView> links)
      throws DatasetException, FeatureStoreMetadataException, MetadataException, ServiceException,
      GenericException, FeaturestoreException, IOException, ModelRegistryException {
    return (ProvExplicitLinkDTO<FeatureViewDTO>)build(uriInfo, resourceRequest, accessProject, user, links);
  }
  
  public ProvExplicitLinkDTO<TrainingDatasetDTO> buildTDLinks(UriInfo uriInfo, ResourceRequest resourceRequest,
                                                          Project accessProject, Users user,
                                                          ProvExplicitLink<TrainingDataset> links)
      throws DatasetException, FeatureStoreMetadataException, MetadataException, ServiceException,
      GenericException, FeaturestoreException, IOException, ModelRegistryException {
    return (ProvExplicitLinkDTO<TrainingDatasetDTO>)build(uriInfo, resourceRequest, accessProject, user, links);
  }
  
  public ProvExplicitLinkDTO<?> build(UriInfo uriInfo, ResourceRequest resourceRequest,
                                      Project accessProject, Users user, ProvExplicitLink<?> links)
      throws GenericException, FeaturestoreException, DatasetException, ServiceException, MetadataException,
      FeatureStoreMetadataException, IOException, ModelRegistryException {
    boolean expandLink = resourceRequest != null && resourceRequest.contains(ResourceRequest.Name.PROVENANCE);
    boolean expandArtifact = resourceRequest != null
      && resourceRequest.contains(ResourceRequest.Name.PROVENANCE_ARTIFACTS);
    if (links.getNode() instanceof FeaturestoreConnector) {
      if (expandLink) {
        return storageConnectorLink(uriInfo, accessProject, user, expandArtifact, links);
      } else {
        ProvExplicitLinkDTO<?> linksDTO = new ProvExplicitLinkDTO<>();
        FeaturestoreConnector connector = (FeaturestoreConnector) links.getNode();
        linksDTO.setHref(storageConnectorURI(uriInfo, accessProject, connector).build());
        return linksDTO;
      }
    } else if (links.getNode() instanceof Featuregroup) {
      if (expandLink) {
        return featureGroupLink(uriInfo, accessProject, user, expandArtifact, links);
      } else {
        ProvExplicitLinkDTO<?> linksDTO = new ProvExplicitLinkDTO<>();
        Featuregroup featureGroup = (Featuregroup) links.getNode();
        linksDTO.setHref(featureGroupURI(uriInfo, accessProject, featureGroup).build());
        return linksDTO;
      }
    } else if (links.getNode() instanceof FeatureView) {
      if (expandLink) {
        return featureViewLink(uriInfo, accessProject, user, expandArtifact, links);
      } else {
        ProvExplicitLinkDTO<?> linksDTO = new ProvExplicitLinkDTO<>();
        FeatureView featureView = (FeatureView) links.getNode();
        linksDTO.setHref(featureViewURI(uriInfo, accessProject, featureView).build());
        return linksDTO;
      }
    } else if (links.getNode() instanceof TrainingDataset) {
      if(expandLink) {
        return trainingDatasetLink(uriInfo, accessProject, user, expandArtifact, links);
      } else {
        ProvExplicitLinkDTO<TrainingDatasetDTO> linksDTO = new ProvExplicitLinkDTO<>();
        TrainingDataset trainingDataset = (TrainingDataset) links.getNode();
        linksDTO.setHref(trainingDatasetURI(uriInfo, accessProject, trainingDataset).build());
        return linksDTO;
      }
    } else if (links.getNode() instanceof ModelVersion) {
      if(expandLink) {
        return modelLink(uriInfo, accessProject, user, expandArtifact, links);
      } else {
        ProvExplicitLinkDTO<ModelDTO> linksDTO = new ProvExplicitLinkDTO<>();
        ModelVersion model = (ModelVersion) links.getNode();
        linksDTO.setHref(modelURI(uriInfo, accessProject, model).build());
        return linksDTO;
      }
    }
    return null;
  }

  private ProvExplicitLinkDTO storageConnectorLink(UriInfo uriInfo, Project accessProject, Users user,
                                                   boolean expandArtifact, ProvExplicitLink links)
      throws FeaturestoreException, ServiceException, MetadataException, FeatureStoreMetadataException,
      DatasetException, IOException, GenericException, ModelRegistryException {
    ProvExplicitLinkDTO<FeaturestoreStorageConnectorDTO> linksDTO = new ProvExplicitLinkDTO<>();
    RestDTO artifactDTO;
    if(links.isDeleted()) {
      ProvArtifact artifact = (ProvArtifact) links.getNode();
      artifactDTO = new ProvArtifactDTO(artifact.getId(), artifact.getProject(),
        artifact.getName(), artifact.getVersion());
    } else {
      FeaturestoreConnector connector = (FeaturestoreConnector) links.getNode();
      linksDTO.setHref(storageConnectorURI(uriInfo, accessProject, connector).build());
      if (links.isAccessible() && expandArtifact) {
        artifactDTO = featurestoreStorageConnectorController.convertToConnectorDTO(user, accessProject, connector);
      } else {
        ProvArtifact artifact = ProvArtifact.fromFeaturestoreConnector(connector);
        artifactDTO = new ProvArtifactDTO(artifact.getId(), artifact.getProject(),
          artifact.getName(), artifact.getVersion());
      }
      URI href =
        featurestoreUtils.storageConnectorByNameURI(uriInfo.getBaseUriBuilder(), accessProject, connector).build();
      artifactDTO.setHref(href);
    }
    linksDTO.setNode(buildNodeDTO(links, artifactDTO));
    traverseLinks(uriInfo, accessProject, user, linksDTO, expandArtifact, links);
    return linksDTO;
  }
  
  private ProvExplicitLinkDTO featureGroupLink(UriInfo uriInfo, Project accessProject, Users user,
                                               boolean expandArtifact, ProvExplicitLink links)
      throws FeaturestoreException, ServiceException, MetadataException, FeatureStoreMetadataException,
      DatasetException, IOException, GenericException, ModelRegistryException {
    ProvExplicitLinkDTO<FeaturegroupDTO> linksDTO = new ProvExplicitLinkDTO<>();
    RestDTO artifactDTO;
    if(links.isDeleted()) {
      ProvArtifact artifact = (ProvArtifact) links.getNode();
      artifactDTO = new ProvArtifactDTO(artifact.getId(), artifact.getProject(),
        artifact.getName(), artifact.getVersion());
    } else {
      Featuregroup featureGroup = (Featuregroup) links.getNode();
      linksDTO.setHref(featureGroupURI(uriInfo, accessProject, featureGroup).build());
      if (links.isAccessible() && expandArtifact) {
        artifactDTO = featuregroupController.convertFeaturegrouptoDTO(featureGroup, accessProject, user);
      } else {
        ProvArtifact artifact = ProvArtifact.fromFeatureGroup(featureGroup);
        artifactDTO = new ProvArtifactDTO(artifact.getId(), artifact.getProject(),
          artifact.getName(), artifact.getVersion());
      }
      URI href =
        featurestoreUtils.featureGroupByIdURI(uriInfo.getBaseUriBuilder(), accessProject, featureGroup).build();
      artifactDTO.setHref(href);
    }
    linksDTO.setNode(buildNodeDTO(links, artifactDTO));
    traverseLinks(uriInfo, accessProject, user, linksDTO, expandArtifact, links);
    return linksDTO;
  }
  
  private ProvExplicitLinkDTO featureViewLink(UriInfo uriInfo, Project accessProject, Users user,
                                              boolean expandArtifact, ProvExplicitLink links)
      throws FeaturestoreException, DatasetException, ServiceException, MetadataException,
      FeatureStoreMetadataException, IOException, GenericException, ModelRegistryException {
    ProvExplicitLinkDTO<FeatureViewDTO> linksDTO = new ProvExplicitLinkDTO<>();
    RestDTO artifactDTO;
    if(links.isDeleted()) {
      ProvArtifact artifact = (ProvArtifact) links.getNode();
      artifactDTO = new ProvArtifactDTO(artifact.getId(), artifact.getProject(),
        artifact.getName(), artifact.getVersion());
      linksDTO.setNode(buildNodeDTO(links, artifactDTO));
    } else {
      FeatureView featureView = (FeatureView) links.getNode();
      linksDTO.setHref(featureViewURI(uriInfo, accessProject, featureView).build());
      if (links.isAccessible() && expandArtifact) {
        ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.FEATUREVIEW);
        resourceRequest.setExpansions(new HashSet<>(Arrays.asList(
          new ResourceRequest(ResourceRequest.Name.QUERY),
          new ResourceRequest(ResourceRequest.Name.FEATURES))));
        try {
          artifactDTO = featureViewBuilder.build(featureView, resourceRequest, accessProject, user, uriInfo);
          linksDTO.setNode(buildNodeDTO(links, artifactDTO));
        } catch (FeaturestoreException e) {
          if (e.getErrorCode().equals(RESTCodes.FeaturestoreErrorCode.FEATUREGROUP_NOT_FOUND)) {
            artifactDTO = new ProvArtifactDTO(featureView.getId(),
              featureView.getFeaturestore().getProject().getName(),
              featureView.getName(), featureView.getVersion());
            linksDTO.setNode(buildNodeDTO(links, artifactDTO, Optional.of(e)));
          } else {
            throw e;
          }
        }
      } else {
        ProvArtifact artifact = ProvArtifact.fromFeatureView(featureView);
        artifactDTO = new ProvArtifactDTO(artifact.getId(), artifact.getProject(),
          artifact.getName(), artifact.getVersion());
        linksDTO.setNode(buildNodeDTO(links, artifactDTO));
      }
      artifactDTO.setHref(
        featurestoreUtils.featureViewURI(uriInfo.getBaseUriBuilder(), accessProject, featureView).build());
    }
    traverseLinks(uriInfo, accessProject, user, linksDTO, expandArtifact, links);
    return linksDTO;
  }
  
  private ProvExplicitLinkDTO trainingDatasetLink(UriInfo uriInfo, Project accessProject, Users user,
                                                  boolean expandArtifact, ProvExplicitLink links)
      throws FeaturestoreException, DatasetException, ServiceException, MetadataException,
      FeatureStoreMetadataException, IOException, GenericException, ModelRegistryException {
    ProvExplicitLinkDTO<TrainingDatasetDTO> linksDTO = new ProvExplicitLinkDTO<>();
    RestDTO artifactDTO;
    
    if (links.isDeleted()) {
      ProvArtifact artifact = (ProvArtifact) links.getNode();
      artifactDTO = new ProvArtifactDTO(artifact.getId(), artifact.getProject(),
        artifact.getName(), artifact.getVersion());
    } else {
      TrainingDataset trainingDataset = (TrainingDataset) links.getNode();
      linksDTO.setHref(trainingDatasetURI(uriInfo, accessProject, trainingDataset).build());
      if (links.isAccessible() && expandArtifact) {
        ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.TRAININGDATASETS);
        artifactDTO =
          trainingDatasetBuilder.build(user, accessProject, trainingDataset, uriInfo, resourceRequest);
      } else {
        ProvArtifact artifact = ProvArtifact.fromTrainingDataset(trainingDataset);
        artifactDTO = new ProvArtifactDTO(artifact.getId(), artifact.getProject(),
          artifact.getName(), artifact.getVersion());
      }
      artifactDTO.setHref(
        featurestoreUtils.trainingDatasetURI(uriInfo.getBaseUriBuilder(), accessProject, trainingDataset).build());
    }
    linksDTO.setNode(buildNodeDTO(links, artifactDTO));
    traverseLinks(uriInfo, accessProject, user, linksDTO, expandArtifact, links);
    return linksDTO;
  }

  private ProvExplicitLinkDTO modelLink(UriInfo uriInfo, Project accessProject, Users user,
                                        boolean expandArtifact, ProvExplicitLink links)
      throws FeaturestoreException, DatasetException, ServiceException, MetadataException,
      FeatureStoreMetadataException, IOException, GenericException, ModelRegistryException {
    ProvExplicitLinkDTO<ModelDTO> linksDTO = new ProvExplicitLinkDTO<>();
    RestDTO artifactDTO;

    if (links.isDeleted()) {
      ProvArtifact artifact = (ProvArtifact) links.getNode();
      artifactDTO = new ProvArtifactDTO(artifact.getId(), artifact.getProject(),
          artifact.getName(), artifact.getVersion());
    } else {
      ModelVersion model = (ModelVersion) links.getNode();
      linksDTO.setHref(modelURI(uriInfo, accessProject, model).build());
      if (links.isAccessible() && expandArtifact) {
        ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.MODELS);
        resourceRequest.setExpansions(new HashSet<>(Arrays.asList(
          new ResourceRequest(ResourceRequest.Name.USERS))));
        artifactDTO = modelsBuilder.build(uriInfo, resourceRequest, user, accessProject, model);
      } else {
        ProvArtifact artifact = ProvArtifact.fromModel(model);
        artifactDTO = new ProvArtifactDTO(artifact.getId(), artifact.getProject(),
            artifact.getName(), artifact.getVersion());
      }
      artifactDTO.setHref(
          modelsBuilder.modelVersionUri(uriInfo, accessProject, model.getModel().getProject(), model)
            .queryParam("expand", "USERS").build());
    }
    linksDTO.setNode(buildNodeDTO(links, artifactDTO));
    traverseLinks(uriInfo, accessProject, user, linksDTO, expandArtifact, links);
    return linksDTO;
  }
  
  private void traverseLinks(UriInfo uriInfo, Project accessProject, Users user, ProvExplicitLinkDTO<?> linksDTO,
                             boolean expandArtifact, ProvExplicitLink<?> links)
      throws FeaturestoreException, ServiceException, MetadataException, FeatureStoreMetadataException,
      DatasetException, IOException, GenericException, ModelRegistryException {
    if(linksDTO.getNode().isAccessible()) {
      for (ProvExplicitLink<?> downstreamLink : links.getDownstream()) {
        ProvExplicitLinkDTO<?> downstreamLinkDTO =
          traverseLinksInt(uriInfo, accessProject, user, expandArtifact, downstreamLink);
        linksDTO.addDownstream(downstreamLinkDTO);
      }
      for (ProvExplicitLink<?> upstreamLink : links.getUpstream()) {
        ProvExplicitLinkDTO<?> upstreamLinkDTO =
          traverseLinksInt(uriInfo, accessProject, user, expandArtifact, upstreamLink);
        linksDTO.addUpstream(upstreamLinkDTO);
      }
    }
  }
  
  private ProvExplicitLinkDTO<?> traverseLinksInt(UriInfo uriInfo, Project accessProject, Users user,
                                                  boolean expandArtifact, ProvExplicitLink link)
      throws FeaturestoreException, ServiceException, DatasetException, MetadataException,
      FeatureStoreMetadataException, IOException, GenericException, ModelRegistryException {
    switch(link.getArtifactType()) {
      case STORAGE_CONNECTOR:
        return storageConnectorLink(uriInfo, accessProject, user, expandArtifact, link);
      case FEATURE_GROUP:
        return featureGroupLink(uriInfo, accessProject, user, expandArtifact, link);
      case FEATURE_VIEW:
        return featureViewLink(uriInfo, accessProject, user, expandArtifact, link);
      case TRAINING_DATASET:
        return trainingDatasetLink(uriInfo, accessProject, user, expandArtifact, link);
      case MODEL:
        return modelLink(uriInfo, accessProject, user, expandArtifact, link);
      default:
        return null;
    }
  }
  private  ProvNodeDTO buildNodeDTO(ProvExplicitLink links, RestDTO artifactDTO) {
    return buildNodeDTO(links, artifactDTO, Optional.empty());
  }
  
  private  ProvNodeDTO buildNodeDTO(ProvExplicitLink links, RestDTO artifactDTO, Optional<RESTException> exception) {
    ProvNodeDTO nodeDTO;
    if(artifactDTO instanceof CachedFeaturegroupDTO) {
      nodeDTO = new ProvCachedFeatureGroupDTO();
    } else if (artifactDTO instanceof StreamFeatureGroupDTO) {
      nodeDTO = new ProvStreamFeatureGroupDTO();
    } else if (artifactDTO instanceof OnDemandFeaturegroupDTO) {
      nodeDTO = new ProvOnDemandFeatureGroupDTO();
    } else {
      nodeDTO = new ProvNodeDTO();
    }
    nodeDTO.setArtifactType(links.getArtifactType());
    nodeDTO.setTraversed(links.isTraversed());
    nodeDTO.setShared(links.isShared());
    nodeDTO.setAccessible(links.isAccessible());
    nodeDTO.setDeleted(links.isDeleted());
    nodeDTO.setArtifact(artifactDTO);
    if(exception.isPresent()) {
      nodeDTO.setExceptionCause(exception.get().getUsrMsg());
    }
    return nodeDTO;
  }
}