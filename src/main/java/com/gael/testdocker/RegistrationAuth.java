package com.gael.testdocker;

import com.spotify.docker.client.messages.RegistryAuth;

public class RegistrationAuth {
	
	public static RegistryAuth getAuth(String AUTH_EMAIL, String AUTH_USERNAME, String AUTH_PASSWORD)
	{
		return RegistryAuth.builder()
				  .email(AUTH_EMAIL)
				  .username(AUTH_USERNAME)
				  .password(AUTH_PASSWORD)
				  .build();
	}
	
}
