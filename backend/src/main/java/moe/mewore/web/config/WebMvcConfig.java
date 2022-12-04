package moe.mewore.web.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${spring.resources.static-locations}")
    private String staticLocations;

    @Value("${spring.resources.chain.cache:true}")
    private boolean shouldCache;

    @Override
    public void addResourceHandlers(final ResourceHandlerRegistry registry) {
        // UI
        registry.addResourceHandler("/**")
                .addResourceLocations(staticLocations)
                .resourceChain(shouldCache);

        // Swagger
        registry.addResourceHandler("/swagger-ui/**", "/swagger-ui")
                .addResourceLocations("classpath:/META-INF/resources/webjars/springfox-swagger-ui/")
                .resourceChain(false);
    }

    @Override
    public void addViewControllers(final ViewControllerRegistry registry) {
        registry.addViewController("/swagger-ui/").setViewName("forward:" + "/swagger-ui/index.html");
    }
}
