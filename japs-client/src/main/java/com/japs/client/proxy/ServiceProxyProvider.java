package com.japs.client.proxy;

import com.japs.annotation.EnableRpcClients;
import com.japs.annotation.RpcService;
import com.japs.registry.ServiceDiscovery;
import com.japs.registry.impl.zookeeper.ZookeeperServiceDiscovery;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.StringUtils;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
public class ServiceProxyProvider implements BeanDefinitionRegistryPostProcessor {

    @NonNull
    private ServiceDiscovery serviceDiscovery;

    @NonNull
    private String[] basePackages;

    public ServiceProxyProvider(ZookeeperServiceDiscovery serviceDiscovery, String[] basePackages) {
        this.serviceDiscovery = serviceDiscovery;
        this.basePackages = basePackages;
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        log.info("register beans");
        ClassPathScanningCandidateComponentProvider scanner = getScanner();
        scanner.addIncludeFilter(new AnnotationTypeFilter(RpcService.class));

        for (String basePackage : basePackages) {
            Set<BeanDefinition> candidateComponents = scanner.findCandidateComponents(basePackage);
            for (BeanDefinition candidateComponent : candidateComponents) {
                if (candidateComponent instanceof AnnotatedBeanDefinition) {
                    AnnotatedBeanDefinition beanDefinition = (AnnotatedBeanDefinition) candidateComponent;
                    AnnotationMetadata annotationMetadata = beanDefinition.getMetadata();

                    BeanDefinitionHolder holder = createBeanDefinition(annotationMetadata);
                    BeanDefinitionReaderUtils.registerBeanDefinition(holder, registry);
                }
            }
        }
    }

    private ClassPathScanningCandidateComponentProvider getScanner() {
        return new ClassPathScanningCandidateComponentProvider(false) {

            @Override
            protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
                if (beanDefinition.getMetadata().isIndependent()) {

                    if (beanDefinition.getMetadata().isInterface()
                            && beanDefinition.getMetadata().getInterfaceNames().length == 1
                            && Annotation.class.getName().equals(beanDefinition.getMetadata().getInterfaceNames()[0])) {

                        try {
                            Class<?> target = Class.forName(beanDefinition.getMetadata().getClassName());
                            return !target.isAnnotation();
                        } catch (Exception ex) {
                            log.error("Could not load target class: {}, {}", beanDefinition.getMetadata().getClassName(), ex);
                        }
                    }
                    return true;
                }
                return false;
            }
        };
    }

    private BeanDefinitionHolder createBeanDefinition(AnnotationMetadata annotationMetadata) {
        String className = annotationMetadata.getClassName();
        log.info("Creating bean definition for class: {}", className);

        BeanDefinitionBuilder definition = BeanDefinitionBuilder.genericBeanDefinition(ProxyFactoryBean.class);
        String beanName = StringUtils.uncapitalize(className.substring(className.lastIndexOf('.') + 1));

        definition.addPropertyValue("type", className);
        definition.addPropertyValue("serviceDiscovery", serviceDiscovery);

        return new BeanDefinitionHolder(definition.getBeanDefinition(), beanName);
    }

    private Set<String> getBasePackages() {
        String[] basePackages = getMainClass().getAnnotation(EnableRpcClients.class).basePackages();
        Set set = new HashSet<>();
        Collections.addAll(set, basePackages);
        return set;
    }

    private Class<?> getMainClass() {
        for (final Map.Entry<String, String> entry : System.getenv().entrySet()) {
            if (entry.getKey().startsWith("JAVA_MAIN_CLASS")) {
                String mainClass = entry.getValue();
                log.debug("Main class: {}", mainClass);
                try {
                    return Class.forName(mainClass);
                } catch (ClassNotFoundException e) {
                    throw new IllegalStateException("Cannot determine main class.");
                }
            }
        }
        throw new IllegalStateException("Cannot determine main class.");
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
    }
}
