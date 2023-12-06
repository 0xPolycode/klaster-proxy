package polycode.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.client.RestTemplate
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.util.DefaultUriBuilderFactory

@Configuration
class WebConfig(private val objectMapper: ObjectMapper) : WebMvcConfigurer {

    @Bean("klasterRestTemplate")
    fun klasterRestTemplate(klasterApiProperties: KlasterApiProperties): RestTemplate =
        RestTemplateBuilder()
            .rootUri(klasterApiProperties.url)
            .uriTemplateHandler(
                DefaultUriBuilderFactory().apply {
                    encodingMode = DefaultUriBuilderFactory.EncodingMode.NONE
                }
            )
            .additionalMessageConverters(MappingJackson2HttpMessageConverter(objectMapper))
            .build()
}
