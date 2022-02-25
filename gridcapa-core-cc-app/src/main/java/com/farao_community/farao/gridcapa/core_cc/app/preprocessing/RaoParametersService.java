/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.gridcapa.core_cc.app.preprocessing;

import com.farao_community.farao.gridcapa.core_cc.app.constants.InputsNamingRules;
import com.farao_community.farao.gridcapa.core_cc.app.inputs.rao_request.Property;
import com.farao_community.farao.gridcapa.core_cc.app.inputs.rao_request.RequestMessage;
import com.farao_community.farao.gridcapa.core_cc.app.messaging.MinioAdapter;
import com.farao_community.farao.rao_api.json.JsonRaoParameters;
import com.farao_community.farao.rao_api.parameters.RaoParameters;
import com.farao_community.farao.search_tree_rao.castor.parameters.SearchTreeRaoParameters;
import com.powsybl.iidm.network.Country;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Mohamed BenRejeb {@literal <mohamed.ben-rejeb at rte-france.com>}
 */
@Service
public class RaoParametersService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RaoParametersService.class);
    private final MinioAdapter minioAdapter;
    private static final String RAO_PARAMETERS_DEFAULT_CONFIGURATION = "rao-default-config.json";
    private static final List<String> GERMAN_ZONES = Arrays.asList("D2", "D4", "D7", "D8");
    private static final String TOPO_RA_MIN_IMPACT = "Topo_RA_Min_impact";
    private static final String PST_RA_MIN_IMPACT = "PST_RA_Min_impact";
    private static final String LOOP_FLOW_COUNTRIES = "LF_Constraint_";
    private static final String MAX_CRA = "Max_cRA_";
    private static final String MAX_TOPO_CRA = "Max_Topo_cRA_";
    private static final String MAX_PST_CRA = "Max_PST_cRA_";

    @Value("${rao-integration.configuration.rao-parameters}")
    private String raoParametersConfigurationPath;

    public RaoParametersService(MinioAdapter minioAdapter) {
        this.minioAdapter = minioAdapter;
    }

    public String uploadJsonRaoParameters(RequestMessage requestMessage, String destinationKey) {
        RaoParameters raoParameters = createRaoParametersFromRequest(requestMessage);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        JsonRaoParameters.write(raoParameters, outputStream);
        String jsonRaoParametersFilePath = String.format(InputsNamingRules.S_INPUTS_S, destinationKey, InputsNamingRules.JSON_RAO_PARAMETERS_FILE_NAME);
        minioAdapter.uploadFile(jsonRaoParametersFilePath, new ByteArrayInputStream(outputStream.toByteArray()));
        return minioAdapter.generatePreSignedUrl(jsonRaoParametersFilePath);
    }

    public RaoParameters createRaoParametersFromRequest(RequestMessage requestMessage) {
        RaoParameters raoParameters;
        try {
            raoParameters = JsonRaoParameters.read(new FileInputStream(raoParametersConfigurationPath));
        } catch (IOException ex1) {
            LOGGER.warn("No rao-parameters JSON file found, use default JSON file instead", ex1);

            try {
                raoParameters = JsonRaoParameters.read(new ClassPathResource(RAO_PARAMETERS_DEFAULT_CONFIGURATION).getInputStream());
            } catch (IOException ex2) {
                LOGGER.warn("Cannot load default JSON rao-parameters config, use farao default configuration instead", ex2);
                raoParameters = new RaoParameters();
            }
        }

        SearchTreeRaoParameters searchTreeRaoParameters = raoParameters.getExtension(SearchTreeRaoParameters.class);
        setLoopFlowCountries(requestMessage, raoParameters);
        setPstPenaltyCost(requestMessage, raoParameters);
        setAbsoluteMinimumImpactThreshold(requestMessage, searchTreeRaoParameters);
        setMaxCurativeRaPerTso(requestMessage, searchTreeRaoParameters);
        setMaxCurativeTopoPerTso(requestMessage, searchTreeRaoParameters);
        setMaxCurativePstPerTso(requestMessage, searchTreeRaoParameters);
        return raoParameters;
    }

    private void setMaxCurativePstPerTso(RequestMessage requestMessage, SearchTreeRaoParameters searchTreeRaoParameters) {
        Map<String, Integer> maxNbrCurativePstByTso = requestMessage.getHeader().getProperty().stream()
                .filter(property -> property.getName().toUpperCase().startsWith(MAX_PST_CRA.toUpperCase()))
                .collect(Collectors.toMap(property -> property.getName().substring(property.getName().lastIndexOf('_') + 1), property -> Integer.parseInt(property.getValue())));
        searchTreeRaoParameters.setMaxCurativePstPerTso(maxNbrCurativePstByTso);
    }

    private void setMaxCurativeTopoPerTso(RequestMessage requestMessage, SearchTreeRaoParameters searchTreeRaoParameters) {
        Map<String, Integer> maxNbrTopoCurRaByTso = requestMessage.getHeader().getProperty().stream()
                .filter(property -> property.getName().toUpperCase().startsWith(MAX_TOPO_CRA.toUpperCase()))
                .collect(Collectors.toMap(property -> property.getName().substring(property.getName().lastIndexOf('_') + 1), property -> Integer.parseInt(property.getValue())));
        searchTreeRaoParameters.setMaxCurativeTopoPerTso(maxNbrTopoCurRaByTso);
    }

    private void setMaxCurativeRaPerTso(RequestMessage requestMessage, SearchTreeRaoParameters searchTreeRaoParameters) {
        Map<String, Integer> maxNbrCurRaByTso = requestMessage.getHeader().getProperty().stream()
                .filter(property -> property.getName().toUpperCase().startsWith(MAX_CRA.toUpperCase()))
                .collect(Collectors.toMap(property -> property.getName().substring(property.getName().lastIndexOf('_') + 1), property -> Integer.parseInt(property.getValue())));
        searchTreeRaoParameters.setMaxCurativeRaPerTso(maxNbrCurRaByTso);
    }

    private void setPstPenaltyCost(RequestMessage requestMessage, RaoParameters raoParameters) {
        Optional<Property> pstRaMinImpactOptional = requestMessage.getHeader().getProperty().stream()
                .filter(property -> property.getName().equalsIgnoreCase(PST_RA_MIN_IMPACT)).findFirst();
        double pstRaMinImpact = 0.0D;
        if (pstRaMinImpactOptional.isPresent()) {
            pstRaMinImpact += Double.parseDouble(pstRaMinImpactOptional.get().getValue());
        }
        raoParameters.setPstPenaltyCost(pstRaMinImpact);
    }

    private void setAbsoluteMinimumImpactThreshold(RequestMessage requestMessage, SearchTreeRaoParameters searchTreeRaoParameters) {
        Optional<Property> topoRaMinImpactOptional = requestMessage.getHeader().getProperty().stream()
                .filter(property -> property.getName().equalsIgnoreCase(TOPO_RA_MIN_IMPACT)).findFirst();
        double topoRaMinImpact = 0.0D;
        if (topoRaMinImpactOptional.isPresent()) {
            topoRaMinImpact += Double.parseDouble(topoRaMinImpactOptional.get().getValue());
        }
        searchTreeRaoParameters.setAbsoluteNetworkActionMinimumImpactThreshold(topoRaMinImpact);
    }

    private void setLoopFlowCountries(RequestMessage requestMessage, RaoParameters raoParameters) {
        List<String> loopFlowZones = requestMessage.getHeader().getProperty().stream()
                .filter(property -> property.getName().toUpperCase().startsWith(LOOP_FLOW_COUNTRIES.toUpperCase()) && property.getValue().equalsIgnoreCase(Boolean.TRUE.toString()))
                .map(property -> property.getName().substring(property.getName().lastIndexOf('_') + 1)).collect(Collectors.toList());
        Set<Country> loopFlowCountries = loopFlowZones.stream().map(this::convertGermanyZones).map(Country::valueOf).collect(Collectors.toSet());
        raoParameters.setLoopflowCountries(loopFlowCountries);
    }

    private String convertGermanyZones(String zone) {
        if (GERMAN_ZONES.contains(zone)) {
            return Country.DE.toString();
        } else {
            return zone;
        }
    }
}
