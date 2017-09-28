package com.gael.testdocker;

import com.spotify.docker.client.messages.RegistryAuth;

/**
 * <p>Class for authentification of Registry</p>
 * 
 * @author bellaiche
 * @version 1.0
 *
 */
public class RegistrationAuth {
	
	/**
	 * Returns an authentification for push or pull
	 * 
	 * @param AUTH_EMAIL Email of registry
	 * @param AUTH_USERNAME Username of registry
	 * @param AUTH_PASSWORD Password of registry
	 * @return instance of RegistryAuth
	 */
	public static RegistryAuth getAuth(String AUTH_EMAIL, String AUTH_USERNAME, String AUTH_PASSWORD)
	{
		return RegistryAuth.builder()
				  .email(AUTH_EMAIL)
				  .username(AUTH_USERNAME)
				  .password(AUTH_PASSWORD)
				  .build();
	}
	
}
