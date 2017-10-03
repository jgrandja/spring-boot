/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.security.oauth2.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ConfigurationCondition;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.oauth2.client.OAuth2ClientTemplatePropertiesLoader;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for a <code>ClientRegistrationRepository</code> bean,
 * which serves as a store for OAuth 2.0 / OpenID Connect 1.0 client registrations.
 *
 * @author Joe Grandja
 * @since 2.0.0
 * @see ClientRegistrationRepository
 */
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({ EnableWebSecurity.class, ClientRegistration.class })
@EnableConfigurationProperties(OAuth2ClientProperties.class)
public class ClientRegistrationRepositoryAutoConfiguration {

	private static final String CLIENT_REGISTRATIONS_PROPERTY_PREFIX = "spring.security.oauth2.client.registrations";

	@Configuration
	@Conditional(ClientsConfiguredCondition.class)
	@ConditionalOnMissingBean(ClientRegistrationRepository.class)
	protected static class ClientRegistrationRepositoryConfiguration {

		private static Map<OAuth2ClientProperties.ClientAuthenticationMethod, ClientAuthenticationMethod> clientAuthenticationMethodMappings;

		private static Map<OAuth2ClientProperties.AuthorizationGrantType, AuthorizationGrantType> authorizationGrantTypeMappings;

		static {
			clientAuthenticationMethodMappings = new HashMap<>();
			clientAuthenticationMethodMappings.put(
					OAuth2ClientProperties.ClientAuthenticationMethod.BASIC,
					ClientAuthenticationMethod.BASIC);
			clientAuthenticationMethodMappings.put(
					OAuth2ClientProperties.ClientAuthenticationMethod.POST,
					ClientAuthenticationMethod.POST);

			authorizationGrantTypeMappings = new HashMap<>();
			authorizationGrantTypeMappings.put(
					OAuth2ClientProperties.AuthorizationGrantType.AUTHORIZATION_CODE,
					AuthorizationGrantType.AUTHORIZATION_CODE);
		}

		@Bean
		public ClientRegistrationRepository clientRegistrationRepository(OAuth2ClientProperties oauth2ClientProperties) {
			this.applyDefaultProperties(oauth2ClientProperties);

			List<ClientRegistration> clientRegistrations = new ArrayList<>();

			oauth2ClientProperties.getRegistrations().entrySet().stream()
					.filter(clientProperties -> !StringUtils.isEmpty(clientProperties.getValue().getClientId()))
					.forEach(clientProperties -> clientRegistrations.add(
							this.mapProperties(clientProperties.getKey(), clientProperties.getValue())));

			return new InMemoryClientRegistrationRepository(clientRegistrations);
		}

		private void applyDefaultProperties(OAuth2ClientProperties oauth2ClientProperties) {
			MutablePropertySources propertySources = new MutablePropertySources();
			propertySources.addLast(
					new MapPropertySource("spring-security-oauth2-client-templates",
							OAuth2ClientTemplatePropertiesLoader.loadClientTemplates()));

			Map<String, OAuth2ClientProperties.ClientRegistration> clientTemplatesProperties =
					new Binder(ConfigurationPropertySources.from(propertySources))
							.bind(OAuth2ClientTemplatePropertiesLoader.CLIENT_TEMPLATES_PROPERTY_PREFIX,
									Bindable.mapOf(String.class, OAuth2ClientProperties.ClientRegistration.class))
							.get();

			oauth2ClientProperties.getRegistrations()
					.forEach((clientRegistrationId, clientProperties) -> {
						// If templateId is not provided (configured) then default to clientRegistrationId
						String templateId = (clientProperties.getTemplateId() != null
								? clientProperties.getTemplateId().toString()
								: clientRegistrationId);
						clientTemplatesProperties.entrySet().stream().filter(clientTemplateProperties ->
								clientTemplateProperties.getKey().equalsIgnoreCase(templateId))
								.findFirst()
								.ifPresent(clientTemplateProperties ->
										this.applyDefaultProperties(clientProperties, clientTemplateProperties.getValue()));
					});
		}

		private void applyDefaultProperties(
				OAuth2ClientProperties.ClientRegistration clientProperties,
				OAuth2ClientProperties.ClientRegistration clientTemplateProperties) {

			// clientAuthenticationMethod
			if (clientProperties.getClientAuthenticationMethod() == null &&
					clientTemplateProperties.getClientAuthenticationMethod() != null) {
				clientProperties.setClientAuthenticationMethod(clientTemplateProperties.getClientAuthenticationMethod());
			}
			// authorizationGrantType
			if (clientProperties.getAuthorizationGrantType() == null &&
					clientTemplateProperties.getAuthorizationGrantType() != null) {
				clientProperties.setAuthorizationGrantType(clientTemplateProperties.getAuthorizationGrantType());
			}
			// redirectUri
			if (StringUtils.isEmpty(clientProperties.getRedirectUri()) &&
					!StringUtils.isEmpty(clientTemplateProperties.getRedirectUri())) {
				clientProperties.setRedirectUri(clientTemplateProperties.getRedirectUri());
			}
			// scope
			if (CollectionUtils.isEmpty(clientProperties.getScope()) &&
					!CollectionUtils.isEmpty(clientTemplateProperties.getScope())) {
				clientProperties.setScope(clientTemplateProperties.getScope());
			}
			// authorizationUri
			if (StringUtils.isEmpty(clientProperties.getAuthorizationUri()) &&
					!StringUtils.isEmpty(clientTemplateProperties.getAuthorizationUri())) {
				clientProperties.setAuthorizationUri(clientTemplateProperties.getAuthorizationUri());
			}
			// tokenUri
			if (StringUtils.isEmpty(clientProperties.getTokenUri()) &&
					!StringUtils.isEmpty(clientTemplateProperties.getTokenUri())) {
				clientProperties.setTokenUri(clientTemplateProperties.getTokenUri());
			}
			// userInfoUri
			if (StringUtils.isEmpty(clientProperties.getUserInfoUri()) &&
					!StringUtils.isEmpty(clientTemplateProperties.getUserInfoUri())) {
				clientProperties.setUserInfoUri(clientTemplateProperties.getUserInfoUri());
			}
			// jwkSetUri
			if (StringUtils.isEmpty(clientProperties.getJwkSetUri()) &&
					!StringUtils.isEmpty(clientTemplateProperties.getJwkSetUri())) {
				clientProperties.setJwkSetUri(clientTemplateProperties.getJwkSetUri());
			}
			// clientName
			if (StringUtils.isEmpty(clientProperties.getClientName()) &&
					!StringUtils.isEmpty(clientTemplateProperties.getClientName())) {
				clientProperties.setClientName(clientTemplateProperties.getClientName());
			}
			// clientAlias
			if (StringUtils.isEmpty(clientProperties.getClientAlias()) &&
					!StringUtils.isEmpty(clientTemplateProperties.getClientAlias())) {
				clientProperties.setClientAlias(clientTemplateProperties.getClientAlias());
			}
		}

		private ClientRegistration mapProperties(String clientRegistrationId, OAuth2ClientProperties.ClientRegistration clientProperties) {
			ClientAuthenticationMethod clientAuthenticationMethod = null;
			if (clientProperties.getClientAuthenticationMethod() != null) {
				clientAuthenticationMethod = clientAuthenticationMethodMappings.get(
						clientProperties.getClientAuthenticationMethod());
			}
			AuthorizationGrantType authorizationGrantType = null;
			if (clientProperties.getAuthorizationGrantType() != null) {
				authorizationGrantType = authorizationGrantTypeMappings.get(
						clientProperties.getAuthorizationGrantType());
			}
			String[] scope = new String[0];
			if (!CollectionUtils.isEmpty(clientProperties.getScope())) {
				scope = clientProperties.getScope().toArray(new String[0]);
			}
			return new ClientRegistration.Builder(clientRegistrationId)
					.clientId(clientProperties.getClientId())
					.clientSecret(clientProperties.getClientSecret())
					.clientAuthenticationMethod(clientAuthenticationMethod)
					.authorizationGrantType(authorizationGrantType)
					.redirectUri(clientProperties.getRedirectUri())
					.scope(scope)
					.authorizationUri(clientProperties.getAuthorizationUri())
					.tokenUri(clientProperties.getTokenUri())
					.userInfoUri(clientProperties.getUserInfoUri())
					.jwkSetUri(clientProperties.getJwkSetUri())
					.clientName(clientProperties.getClientName())
					.build();
		}
	}

	protected static class ClientsConfiguredCondition extends SpringBootCondition implements ConfigurationCondition {

		@Override
		public ConfigurationCondition.ConfigurationPhase getConfigurationPhase() {
			return ConfigurationPhase.PARSE_CONFIGURATION;
		}

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
			ConditionMessage.Builder message = ConditionMessage.forCondition("OAuth2 Clients Configured Condition");
			Set<String> clientRegistrationIds = this.getClientRegistrationIds(context.getEnvironment());
			if (!clientRegistrationIds.isEmpty()) {
				return ConditionOutcome.match(message.foundExactly("OAuth2 Client(s) -> "
						+ clientRegistrationIds.stream().collect(Collectors.joining(", "))));
			}
			return ConditionOutcome.noMatch(message.notAvailable("OAuth2 Client(s)"));
		}

		private Set<String> getClientRegistrationIds(Environment environment) {
			Map<String, OAuth2ClientProperties.ClientRegistration> clientsProperties = Binder.get(environment)
					.bind(CLIENT_REGISTRATIONS_PROPERTY_PREFIX,
							Bindable.mapOf(String.class, OAuth2ClientProperties.ClientRegistration.class))
					.orElse(new HashMap<>());

			// Filter out clients that don't have the client-id property set
			return clientsProperties.entrySet().stream()
					.filter(e -> !StringUtils.isEmpty(e.getValue().getClientId()))
					.map(Map.Entry::getKey)
					.collect(Collectors.toSet());
		}
	}
}
