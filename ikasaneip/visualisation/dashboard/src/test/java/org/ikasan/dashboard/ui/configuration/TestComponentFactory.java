package org.ikasan.dashboard.ui.configuration;

import org.apache.catalina.Context;
import org.apache.tomcat.util.scan.StandardJarScanner;
import org.ikasan.spec.metadata.*;
import org.ikasan.spec.module.ModuleType;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.List;

@Profile("test")
@Configuration
public class TestComponentFactory
{
    @Bean
    public TomcatServletWebServerFactory tomcatFactory() {
        return new TomcatServletWebServerFactory() {
            @Override
            protected void postProcessContext(Context context) {
                ((StandardJarScanner) context.getJarScanner()).setScanManifest(false);
            }
        };
    }

    @Bean({"moduleMetadataService"})
    public ModuleMetaDataService moduleMetadataService()
    {
        return new ModuleMetaDataService()
        {
            @Override
            public ModuleMetaData findById(String id)
            {
                return null;
            }

            @Override
            public List<ModuleMetaData> findAll()
            {
                return null;
            }

            @Override
            public ModuleMetadataSearchResults find(List<String> modulesNames, Integer startOffset, Integer resultSize) {
                return null;
            }

            @Override
            public ModuleMetadataSearchResults find(List<String> modulesNames, ModuleType moduleType, Integer startOffset, Integer resultSize) {
                return null;
            }

            @Override
            public void deleteById(String name) {

            }

            @Override
            public ModuleMetadataSearchResults find(List<String> modulesNames) {
                return null;
            }
        };
    }

    @Bean({"configurationMetadataService"})
    public ConfigurationMetaDataService configurationMetadataService()
    {
        return new ConfigurationMetaDataService()
        {

            @Override
            public ConfigurationMetaData findById(String id)
            {
                return null;
            }

            @Override
            public List<ConfigurationMetaData> findAll()
            {
                return null;
            }

            @Override
            public List<ConfigurationMetaData> findByIdList(List<String> configurationIds)
            {
                return null;
            }
        };
    }

}
