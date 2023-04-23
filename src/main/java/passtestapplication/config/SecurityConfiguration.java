package passtestapplication.config;

import com.github.javafaker.Faker;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.BeanIds;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import passtestapplication.service.Impl.AuthenticationServiceImpl;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Collections;

@EnableWebSecurity
@Configuration
public class SecurityConfiguration extends WebSecurityConfigurerAdapter implements WebMvcConfigurer {

    private static final String[] WHITE_LIST = {
            "/api/v1/auth/login",
            "/api/v1/news/find_by_id/*",
            "/api/v1/news/paging",
            "/api/v1/news/selected",
            "/api/v1/image/find_by_id/*",
            "/api/v1/category/find_all",
            "/api/v1/category/find_by_id/*",
            "/api/v1/basic_info/find_by_id/*",
            // -- Swagger UI v2
            "/v2/api-docs",
            "/swagger-resources",
            "/swagger-resources/**",
            "/configuration/ui",
            "/configuration/security",
            "/swagger-ui.html",
            "/webjars/**",
            // -- Swagger UI v3 (OpenAPI)
            "/v3/api-docs/**",
            "/swagger-ui/**"
    };


    private final AuthenticationServiceImpl myUserService;

    public SecurityConfiguration(@Lazy AuthenticationServiceImpl myUserService) {
        this.myUserService = myUserService;
    }

    @Bean
    public CommandLineRunner commandLineRunner(){
        return args -> {};
    }

    @Bean
    public Faker faker(){
        return new Faker();
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(myUserService).passwordEncoder(passwordEncoder());
    }


    @Bean(name = BeanIds.AUTHENTICATION_MANAGER)
    @Override
    protected AuthenticationManager authenticationManager() throws Exception {
        return super.authenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .cors()
                .configurationSource(request -> {
                    CorsConfiguration configuration = new CorsConfiguration();
                         configuration.setAllowedOriginPatterns(Collections.singletonList("*"));
                         configuration.addAllowedMethod(HttpMethod.GET);
                         configuration.addAllowedMethod(HttpMethod.PUT);
                         configuration.addAllowedMethod(HttpMethod.POST);
                         configuration.addAllowedMethod(HttpMethod.DELETE);
                         configuration.setAllowedHeaders(Collections.singletonList("*"));
                         configuration.setAllowCredentials(true);
                    return configuration;
        })
                .and()
                .csrf()
                .disable()
                .authorizeRequests(
                        authorize ->
                                authorize.antMatchers(WHITE_LIST)
                                        .permitAll()
                                        .anyRequest()
                                        .authenticated())
                .oauth2ResourceServer(OAuth2ResourceServerConfigurer::jwt)
                .sessionManagement(
                        strategy ->
                                strategy.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(
                        configurer ->
                                configurer
                                        .authenticationEntryPoint(new BearerTokenAuthenticationEntryPoint())
                                        .accessDeniedHandler(new BearerTokenAccessDeniedHandler()));
    }



    @Value("${general.public.key}")
    RSAPublicKey publicKey;

    @Value("${general.private.key}")
    RSAPrivateKey privateKey;

    @Bean
    public JwtEncoder jwtEncoder() {
        JWK jwk = new RSAKey.Builder(this.publicKey).privateKey(this.privateKey).build();
        JWKSource<SecurityContext> contextJWKSource = new ImmutableJWKSet<>(new JWKSet(jwk));
        return new NimbusJwtEncoder(contextJWKSource);
    }

    @Bean
    public JwtDecoder decoder(OAuth2ResourceServerProperties properties){
        return NimbusJwtDecoder
                .withPublicKey(publicKey).build();
    }


}
