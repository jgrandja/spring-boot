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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.security.SecurityAutoConfiguration;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

/**
 * Tests for {@link ClientRegistrationRepositoryAutoConfiguration}.
 *
 * @author Joe Grandja
 */
public class ClientRegistrationRepositoryAutoConfigurationTests {

	private static final String CLIENT_REGISTRATIONS_PROPERTY_PREFIX = "spring.security.oauth2.client.registrations";

	private static final String TEMPLATE_ID_PROPERTY = "template-id";

	private static final String CLIENT_ID_PROPERTY = "client-id";

	private static final String CLIENT_SECRET_PROPERTY = "client-secret";

	private static final String DEFAULT_REDIRECT_URI = "{scheme}://{serverName}:{serverPort}{contextPath}/oauth2/authorize/code/{registrationId}";

	private static final String GOOGLE_REGISTRATION_ID = "google";

	private static final String GOOGLE_CLIENT_PROPERTY_BASE = CLIENT_REGISTRATIONS_PROPERTY_PREFIX + "."
			+ GOOGLE_REGISTRATION_ID;

	private static final String GITHUB_REGISTRATION_ID = "github";

	private static final String GITHUB_CLIENT_PROPERTY_BASE = CLIENT_REGISTRATIONS_PROPERTY_PREFIX + "."
			+ GITHUB_REGISTRATION_ID;

	private static final String OKTA_REGISTRATION_ID = "okta";

	private static final String OKTA_CLIENT_PROPERTY_BASE = CLIENT_REGISTRATIONS_PROPERTY_PREFIX + "."
			+ OKTA_REGISTRATION_ID;

	private static final String FACEBOOK_REGISTRATION_ID = "facebook";

	private static final String FACEBOOK_CLIENT_PROPERTY_BASE = CLIENT_REGISTRATIONS_PROPERTY_PREFIX
			+ "." + FACEBOOK_REGISTRATION_ID;

	private AnnotationConfigWebApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void refreshContextWhenNoOverridesThenLoadDefaultConfiguration()
			throws Exception {
		this.prepareContext(DefaultConfiguration.class);

		// Prepare environment
		TestPropertyValues
				.of(GOOGLE_CLIENT_PROPERTY_BASE + "." + CLIENT_ID_PROPERTY
						+ "=google-client-id")
				.and(GOOGLE_CLIENT_PROPERTY_BASE + "." + CLIENT_SECRET_PROPERTY
						+ "=google-client-secret")
				.and(GOOGLE_CLIENT_PROPERTY_BASE + "." + TEMPLATE_ID_PROPERTY
						+ "=" + GOOGLE_REGISTRATION_ID)
				.and(GITHUB_CLIENT_PROPERTY_BASE + "." + CLIENT_ID_PROPERTY
						+ "=github-client-id")
				.and(GITHUB_CLIENT_PROPERTY_BASE + "." + CLIENT_SECRET_PROPERTY
						+ "=github-client-secret")
				.and(GITHUB_CLIENT_PROPERTY_BASE + "." + TEMPLATE_ID_PROPERTY
						+ "=" + GITHUB_REGISTRATION_ID)
				.and(OKTA_CLIENT_PROPERTY_BASE + "." + CLIENT_ID_PROPERTY
						+ "=okta-client-id")
				.and(OKTA_CLIENT_PROPERTY_BASE + "." + CLIENT_SECRET_PROPERTY
						+ "=okta-client-secret")
				.and(OKTA_CLIENT_PROPERTY_BASE + "." + TEMPLATE_ID_PROPERTY
						+ "=" + OKTA_REGISTRATION_ID)
				.and(OKTA_CLIENT_PROPERTY_BASE
						+ ".authorization-uri=https://your-subdomain.oktapreview.com/oauth2/v1/authorize")
				.and(OKTA_CLIENT_PROPERTY_BASE
						+ ".token-uri=https://your-subdomain.oktapreview.com/oauth2/v1/token")
				.and(OKTA_CLIENT_PROPERTY_BASE
						+ ".user-info-uri=https://your-subdomain.oktapreview.com/oauth2/v1/userinfo")
				.and(OKTA_CLIENT_PROPERTY_BASE
						+ ".jwk-set-uri=https://your-subdomain.oktapreview.com/oauth2/v1/keys")
				.and(FACEBOOK_CLIENT_PROPERTY_BASE + "." + CLIENT_ID_PROPERTY
						+ "=facebook-client-id")
				.and(FACEBOOK_CLIENT_PROPERTY_BASE + "." + CLIENT_SECRET_PROPERTY
						+ "=facebook-client-secret")
				.and(FACEBOOK_CLIENT_PROPERTY_BASE + "." + TEMPLATE_ID_PROPERTY
						+ "=" + FACEBOOK_REGISTRATION_ID)
				.applyTo(this.context.getEnvironment());

		this.context.refresh();

		ClientRegistrationRepository clientRegistrationRepository = this
				.getBean(ClientRegistrationRepository.class);
		assertThat(clientRegistrationRepository).isNotNull();

		ClientRegistration googleClientRegistration = clientRegistrationRepository
				.findByRegistrationId(GOOGLE_REGISTRATION_ID);
		assertThat(googleClientRegistration).isNotNull();
		assertThat(googleClientRegistration.getClientId()).isEqualTo("google-client-id");
		assertThat(googleClientRegistration.getClientSecret())
				.isEqualTo("google-client-secret");
		this.assertGoogleClientPropertyDefaults(googleClientRegistration);

		ClientRegistration gitHubClientRegistration = clientRegistrationRepository
				.findByRegistrationId(GITHUB_REGISTRATION_ID);
		assertThat(gitHubClientRegistration).isNotNull();
		assertThat(gitHubClientRegistration.getClientId()).isEqualTo("github-client-id");
		assertThat(gitHubClientRegistration.getClientSecret())
				.isEqualTo("github-client-secret");
		this.assertGitHubClientPropertyDefaults(gitHubClientRegistration);

		ClientRegistration oktaClientRegistration = clientRegistrationRepository
				.findByRegistrationId(OKTA_REGISTRATION_ID);
		assertThat(oktaClientRegistration).isNotNull();
		assertThat(oktaClientRegistration.getClientId()).isEqualTo("okta-client-id");
		assertThat(oktaClientRegistration.getClientSecret())
				.isEqualTo("okta-client-secret");
		assertThat(oktaClientRegistration.getProviderDetails().getAuthorizationUri())
				.isEqualTo("https://your-subdomain.oktapreview.com/oauth2/v1/authorize");
		assertThat(oktaClientRegistration.getProviderDetails().getTokenUri())
				.isEqualTo("https://your-subdomain.oktapreview.com/oauth2/v1/token");
		assertThat(oktaClientRegistration.getProviderDetails().getUserInfoEndpoint().getUri())
				.isEqualTo("https://your-subdomain.oktapreview.com/oauth2/v1/userinfo");
		assertThat(oktaClientRegistration.getProviderDetails().getJwkSetUri())
				.isEqualTo("https://your-subdomain.oktapreview.com/oauth2/v1/keys");
		this.assertOktaClientPropertyDefaults(oktaClientRegistration);

		ClientRegistration facebookClientRegistration = clientRegistrationRepository
				.findByRegistrationId(FACEBOOK_REGISTRATION_ID);
		assertThat(facebookClientRegistration).isNotNull();
		assertThat(facebookClientRegistration.getClientId())
				.isEqualTo("facebook-client-id");
		assertThat(facebookClientRegistration.getClientSecret())
				.isEqualTo("facebook-client-secret");
		this.assertFacebookClientPropertyDefaults(facebookClientRegistration);
	}

	@Test
	public void refreshContextWhenGoogleClientConfiguredNoOverridesThenLoadDefaultConfiguration()
			throws Exception {
		this.prepareContext(DefaultConfiguration.class);

		// Prepare environment
		TestPropertyValues
				.of(GOOGLE_CLIENT_PROPERTY_BASE + "." + CLIENT_ID_PROPERTY
						+ "=google-client-id")
				.and(GOOGLE_CLIENT_PROPERTY_BASE + "." + CLIENT_SECRET_PROPERTY
						+ "=google-client-secret")
				.and(GOOGLE_CLIENT_PROPERTY_BASE + "." + TEMPLATE_ID_PROPERTY
						+ "=" + GOOGLE_REGISTRATION_ID)
				.applyTo(this.context.getEnvironment());

		this.context.refresh();

		ClientRegistrationRepository clientRegistrationRepository = this
				.getBean(ClientRegistrationRepository.class);
		assertThat(clientRegistrationRepository).isNotNull();

		ClientRegistration googleClientRegistration = clientRegistrationRepository
				.findByRegistrationId(GOOGLE_REGISTRATION_ID);
		assertThat(googleClientRegistration).isNotNull();
		assertThat(googleClientRegistration.getClientId()).isEqualTo("google-client-id");
		assertThat(googleClientRegistration.getClientSecret())
				.isEqualTo("google-client-secret");
		this.assertGoogleClientPropertyDefaults(googleClientRegistration);
	}

	@Test
	public void refreshContextWhenGitHubCustomRegistrationIdConfiguredNoOverridesThenLoadDefaultConfiguration()
			throws Exception {
		this.prepareContext(DefaultConfiguration.class);

		String customRegistrationId = CLIENT_REGISTRATIONS_PROPERTY_PREFIX + "." + "github-custom";

		// Prepare environment
		TestPropertyValues
				.of(customRegistrationId + "." + CLIENT_ID_PROPERTY
						+ "=github-client-id")
				.and(customRegistrationId + "." + CLIENT_SECRET_PROPERTY
						+ "=github-client-secret")
				.and(customRegistrationId + "." + TEMPLATE_ID_PROPERTY
						+ "=" + GITHUB_REGISTRATION_ID)
				.applyTo(this.context.getEnvironment());

		this.context.refresh();

		ClientRegistrationRepository clientRegistrationRepository = this
				.getBean(ClientRegistrationRepository.class);
		assertThat(clientRegistrationRepository).isNotNull();

		ClientRegistration githubClientRegistration = clientRegistrationRepository
				.findByRegistrationId("github-custom");
		assertThat(githubClientRegistration).isNotNull();
		assertThat(githubClientRegistration.getClientId()).isEqualTo("github-client-id");
		assertThat(githubClientRegistration.getClientSecret())
				.isEqualTo("github-client-secret");
		this.assertGitHubClientPropertyDefaults(githubClientRegistration);
	}

	@Test
	public void refreshContextWhenGitHubCustomRegistrationIdConfiguredAndOverridesThenLoadCustomConfiguration()
			throws Exception {
		this.prepareContext(DefaultConfiguration.class);

		String customRegistrationId = CLIENT_REGISTRATIONS_PROPERTY_PREFIX + "." + "github-custom";

		// Prepare environment
		TestPropertyValues
				.of(customRegistrationId + "." + CLIENT_ID_PROPERTY
						+ "=github-client-id")
				.and(customRegistrationId + "." + CLIENT_SECRET_PROPERTY
						+ "=github-client-secret")
				.and(customRegistrationId + "." + TEMPLATE_ID_PROPERTY
						+ "=" + GITHUB_REGISTRATION_ID)
				.and(customRegistrationId + ".scope"
						+ "=scope1, scope2, scope3")
				.and(customRegistrationId + ".client-authentication-method"
						+ "=post")
				.and(customRegistrationId + ".redirect-uri"
						+ "=https://localhost:8080/callback/github-custom")
				.and(customRegistrationId + ".client-name"
						+ "=GitHub Custom")
				.applyTo(this.context.getEnvironment());

		this.context.refresh();

		ClientRegistrationRepository clientRegistrationRepository = this
				.getBean(ClientRegistrationRepository.class);
		assertThat(clientRegistrationRepository).isNotNull();

		ClientRegistration githubClientRegistration = clientRegistrationRepository
				.findByRegistrationId("github-custom");
		assertThat(githubClientRegistration).isNotNull();
		assertThat(githubClientRegistration.getClientId()).isEqualTo("github-client-id");
		assertThat(githubClientRegistration.getClientSecret())
				.isEqualTo("github-client-secret");
		assertThat(githubClientRegistration.getScope()).isEqualTo(
				Stream.of("scope1", "scope2", "scope3").collect(Collectors.toSet()));
		assertThat(githubClientRegistration.getClientAuthenticationMethod())
				.isEqualTo(ClientAuthenticationMethod.POST);
		assertThat(githubClientRegistration.getRedirectUri()).isEqualTo("https://localhost:8080/callback/github-custom");
		assertThat(githubClientRegistration.getClientName()).isEqualTo("GitHub Custom");
	}

	@Test
	public void refreshContextWhenGoogleClientConfiguredWithoutTemplateIdNoOverridesThenLoadDefaultConfiguration()
			throws Exception {
		this.prepareContext(DefaultConfiguration.class);

		// Prepare environment
		TestPropertyValues
				.of(GOOGLE_CLIENT_PROPERTY_BASE + "." + CLIENT_ID_PROPERTY
						+ "=google-client-id")
				.and(GOOGLE_CLIENT_PROPERTY_BASE + "." + CLIENT_SECRET_PROPERTY
						+ "=google-client-secret")
				.applyTo(this.context.getEnvironment());

		this.context.refresh();

		ClientRegistrationRepository clientRegistrationRepository = this
				.getBean(ClientRegistrationRepository.class);
		assertThat(clientRegistrationRepository).isNotNull();

		ClientRegistration googleClientRegistration = clientRegistrationRepository
				.findByRegistrationId(GOOGLE_REGISTRATION_ID);
		assertThat(googleClientRegistration).isNotNull();
		assertThat(googleClientRegistration.getClientId()).isEqualTo("google-client-id");
		assertThat(googleClientRegistration.getClientSecret())
				.isEqualTo("google-client-secret");
		this.assertGoogleClientPropertyDefaults(googleClientRegistration);
	}

	@Test(expected = NoSuchBeanDefinitionException.class)
	public void refreshContextWhenNotWebContextThenConfigurationBacksOff()
			throws Exception {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(ClientRegistrationRepositoryAutoConfiguration.class,
				SecurityAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		context.refresh();
		context.getBean(ClientRegistrationRepository.class);
	}

	@Test
	public void refreshContextWhenNoClientsConfiguredThenConfigurationBacksOff()
			throws Exception {
		this.prepareContext(DefaultConfiguration.class);
		this.context.refresh();
		ClientRegistrationRepository clientRegistrationRepository = this
				.getBean(ClientRegistrationRepository.class);
		assertThat(clientRegistrationRepository).isNull();
	}

	@Test
	public void refreshContextWhenOneInvalidClientConfiguredThenConfigurationBacksOff()
			throws Exception {
		this.prepareContext(DefaultConfiguration.class);

		// Prepare environment
		// NOTE: client-id is required, if not configured, the client will get filtered
		// out
		TestPropertyValues.of(GITHUB_CLIENT_PROPERTY_BASE + "." + CLIENT_SECRET_PROPERTY
				+ "=github-client-secret").applyTo(this.context.getEnvironment());

		this.context.refresh();
		ClientRegistrationRepository clientRegistrationRepository = this
				.getBean(ClientRegistrationRepository.class);
		assertThat(clientRegistrationRepository).isNull();
	}

	@Test
	public void refreshContextWhenTwoInvalidClientsConfiguredThenConfigurationBacksOff()
			throws Exception {
		this.prepareContext(DefaultConfiguration.class);

		// Prepare environment
		// NOTE: client-id is required, if not configured, the client will get filtered
		// out
		TestPropertyValues
				.of(OKTA_CLIENT_PROPERTY_BASE + "." + CLIENT_SECRET_PROPERTY
						+ "=okta-client-secret")
				.and(GOOGLE_CLIENT_PROPERTY_BASE + "." + CLIENT_SECRET_PROPERTY
						+ "=google-client-secret")
				.applyTo(this.context.getEnvironment());

		this.context.refresh();
		ClientRegistrationRepository clientRegistrationRepository = this
				.getBean(ClientRegistrationRepository.class);
		assertThat(clientRegistrationRepository).isNull();
	}

	@Test
	public void refreshContextWhenClientRegistrationRepositoryOverrideThenConfigurationBacksOff()
			throws Exception {
		ClientRegistrationRepository expectedClientRegistrationRepository = Mockito.mock(
				ClientRegistrationRepository.class);
		CustomConfiguration.clientRegistrationRepository = expectedClientRegistrationRepository;

		this.prepareContext(CustomConfiguration.class);

		// Prepare environment
		TestPropertyValues
				.of(FACEBOOK_CLIENT_PROPERTY_BASE + "." + CLIENT_ID_PROPERTY
						+ "=facebook-client-id")
				.and(FACEBOOK_CLIENT_PROPERTY_BASE + "." + CLIENT_SECRET_PROPERTY
						+ "=facebook-client-secret")
				.and(FACEBOOK_CLIENT_PROPERTY_BASE + "." + TEMPLATE_ID_PROPERTY
						+ "=" + FACEBOOK_REGISTRATION_ID)
				.applyTo(this.context.getEnvironment());

		this.context.refresh();

		ClientRegistrationRepository clientRegistrationRepository = this
				.getBean(ClientRegistrationRepository.class);
		assertThat(clientRegistrationRepository).isNotNull();
		assertThat(clientRegistrationRepository)
				.isSameAs(expectedClientRegistrationRepository);
	}

	private void assertGoogleClientPropertyDefaults(
			ClientRegistration clientRegistration) {
		assertThat(clientRegistration.getClientAuthenticationMethod())
				.isEqualTo(ClientAuthenticationMethod.BASIC);
		assertThat(clientRegistration.getAuthorizationGrantType())
				.isEqualTo(AuthorizationGrantType.AUTHORIZATION_CODE);
		assertThat(clientRegistration.getRedirectUri()).isEqualTo(DEFAULT_REDIRECT_URI);
		assertThat(clientRegistration.getScope())
				.isEqualTo(Stream.of("openid", "profile", "email", "address", "phone")
						.collect(Collectors.toSet()));
		assertThat(clientRegistration.getProviderDetails().getAuthorizationUri())
				.isEqualTo("https://accounts.google.com/o/oauth2/v2/auth");
		assertThat(clientRegistration.getProviderDetails().getTokenUri())
				.isEqualTo("https://www.googleapis.com/oauth2/v4/token");
		assertThat(clientRegistration.getProviderDetails().getUserInfoEndpoint().getUri())
				.isEqualTo("https://www.googleapis.com/oauth2/v3/userinfo");
		assertThat(clientRegistration.getProviderDetails().getJwkSetUri())
				.isEqualTo("https://www.googleapis.com/oauth2/v3/certs");
		assertThat(clientRegistration.getClientName()).isEqualTo("Google");
	}

	private void assertGitHubClientPropertyDefaults(
			ClientRegistration clientRegistration) {
		assertThat(clientRegistration.getClientAuthenticationMethod())
				.isEqualTo(ClientAuthenticationMethod.BASIC);
		assertThat(clientRegistration.getAuthorizationGrantType())
				.isEqualTo(AuthorizationGrantType.AUTHORIZATION_CODE);
		assertThat(clientRegistration.getRedirectUri()).isEqualTo("{baseRedirectUrl}/oauth2/authorize/code/{registrationId}");
		assertThat(clientRegistration.getScope())
				.isEqualTo(Stream.of("user").collect(Collectors.toSet()));
		assertThat(clientRegistration.getProviderDetails().getAuthorizationUri())
				.isEqualTo("https://github.com/login/oauth/authorize");
		assertThat(clientRegistration.getProviderDetails().getTokenUri())
				.isEqualTo("https://github.com/login/oauth/access_token");
		assertThat(clientRegistration.getProviderDetails().getUserInfoEndpoint().getUri())
				.isEqualTo("https://api.github.com/user");
		assertThat(clientRegistration.getProviderDetails().getJwkSetUri())
				.isNullOrEmpty();
		assertThat(clientRegistration.getClientName()).isEqualTo("GitHub");
	}

	private void assertOktaClientPropertyDefaults(ClientRegistration clientRegistration) {
		assertThat(clientRegistration.getClientAuthenticationMethod())
				.isEqualTo(ClientAuthenticationMethod.BASIC);
		assertThat(clientRegistration.getAuthorizationGrantType())
				.isEqualTo(AuthorizationGrantType.AUTHORIZATION_CODE);
		assertThat(clientRegistration.getRedirectUri()).isEqualTo(DEFAULT_REDIRECT_URI);
		assertThat(clientRegistration.getScope())
				.isEqualTo(Stream.of("openid", "profile", "email", "address", "phone")
						.collect(Collectors.toSet()));
		assertThat(clientRegistration.getClientName()).isEqualTo("Okta");
	}

	private void assertFacebookClientPropertyDefaults(
			ClientRegistration clientRegistration) {
		assertThat(clientRegistration.getClientAuthenticationMethod())
				.isEqualTo(ClientAuthenticationMethod.POST);
		assertThat(clientRegistration.getAuthorizationGrantType())
				.isEqualTo(AuthorizationGrantType.AUTHORIZATION_CODE);
		assertThat(clientRegistration.getRedirectUri()).isEqualTo(DEFAULT_REDIRECT_URI);
		assertThat(clientRegistration.getScope()).isEqualTo(
				Stream.of("public_profile", "email").collect(Collectors.toSet()));
		assertThat(clientRegistration.getProviderDetails().getAuthorizationUri())
				.isEqualTo("https://www.facebook.com/v2.8/dialog/oauth");
		assertThat(clientRegistration.getProviderDetails().getTokenUri())
				.isEqualTo("https://graph.facebook.com/v2.8/oauth/access_token");
		assertThat(clientRegistration.getProviderDetails().getUserInfoEndpoint().getUri())
				.isEqualTo("https://graph.facebook.com/me");
		assertThat(clientRegistration.getProviderDetails().getJwkSetUri())
				.isNullOrEmpty();
		assertThat(clientRegistration.getClientName()).isEqualTo("Facebook");
	}

	private void prepareContext(Class<?>... configurationClasses) {
		this.context = new AnnotationConfigWebApplicationContext();
		if (configurationClasses != null) {
			this.context.register(configurationClasses);
		}
		this.context.register(ClientRegistrationRepositoryAutoConfiguration.class,
				SecurityAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class);
	}

	private <T> T getBean(Class<T> requiredType) {
		try {
			return this.context.getBean(requiredType);
		}
		catch (BeansException ex) {
			return null;
		}
	}

	@Configuration
	protected static class DefaultConfiguration {
	}

	@Configuration
	protected static class CustomConfiguration {
		private static ClientRegistrationRepository clientRegistrationRepository;

		@Bean
		protected ClientRegistrationRepository clientRegistrationRepository() {
			return clientRegistrationRepository;
		}
	}
}
