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

import java.util.Map;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.security.SecurityAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.oauth2.client.OAuth2ClientTemplatePropertiesLoader;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for OAuth 2.0 / OpenID Connect 1.0 login support.
 *
 * @author Joe Grandja
 * @since 2.0.0
 * @see HttpSecurity#oauth2Login()
 */
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({EnableWebSecurity.class, ClientRegistration.class})
@ConditionalOnMissingBean(WebSecurityConfigurerAdapter.class)
@ConditionalOnBean(ClientRegistrationRepository.class)
@AutoConfigureBefore(SecurityAutoConfiguration.class)
@AutoConfigureAfter(ClientRegistrationRepositoryAutoConfiguration.class)
public class OAuth2LoginAutoConfiguration {

	private static final String USER_NAME_ATTR_NAME_PROPERTY = "user-name-attribute-name";

	@Configuration
	protected static class OAuth2LoginConfiguration extends WebSecurityConfigurerAdapter {
		private final ClientRegistrationRepository clientRegistrationRepository;

		private final OAuth2ClientProperties oauth2ClientProperties;

		private final PropertySource<Map<String, Object>> clientTemplatesPropertySource;

		protected OAuth2LoginConfiguration(
				ClientRegistrationRepository clientRegistrationRepository,
				OAuth2ClientProperties oauth2ClientProperties) {

			this.clientRegistrationRepository = clientRegistrationRepository;
			this.oauth2ClientProperties = oauth2ClientProperties;
			this.clientTemplatesPropertySource = new MapPropertySource(
					"spring-security-oauth2-client-templates",
					OAuth2ClientTemplatePropertiesLoader.loadClientTemplates());
		}

		// @formatter:off
		@Override
		protected void configure(HttpSecurity http) throws Exception {
			http
				.authorizeRequests()
					.anyRequest().authenticated()
					.and()
				.oauth2Login()
					.clients(this.clientRegistrationRepository);
		}
		// @formatter:on

	}
}
