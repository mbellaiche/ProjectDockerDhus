package com.gael.testdocker;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.DockerClient.BuildParam;
import com.spotify.docker.client.DockerClient.ListContainersParam;
import com.spotify.docker.client.DockerClient.ListImagesParam;
import com.spotify.docker.client.LogStream;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.Container;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.ExecCreation;
import com.spotify.docker.client.messages.HostConfig;
import com.spotify.docker.client.messages.HostConfig.Bind;
import com.spotify.docker.client.messages.Image;
import com.spotify.docker.client.messages.PortBinding;
import com.spotify.docker.client.messages.RegistryAuth;

public class DockerCommands {
	
	private DockerClient docker;
	
	public DockerCommands(DockerClient docker)
	{
		this.docker = docker;
	}
	
	public DockerCommands() throws DockerCertificateException
	{
		this(DefaultDockerClient.fromEnv().build());
	}
	
	public DockerClient getDockerClient()
	{
		return this.docker;
	}
	
	public void close()
	{
		this.docker.close();
	}
	
	/*
	 *	METHODS FOR IMAGES
	 */
	
	public boolean existImage(String nameImage) throws DockerException, InterruptedException
	{
		List<Image> images = this.docker.listImages(ListImagesParam.allImages());
		
		if (images != null)
		{
			for (Image i : images)
			{
				ImmutableList<String> tags = i.repoTags();
				for (String tag : tags)
				{
					if (tag.substring(0, tag.indexOf(":")).equals(nameImage))
					{
						return true;
					}
				}
			}
		}
		return false;
	}
	
	public void pullImage(String tagImage) throws DockerException, InterruptedException
	{
		this.docker.pull(tagImage);
	}
	
	public void pushImage(String tagImage) throws DockerException, InterruptedException
	{
		this.docker.push(tagImage);
	}
	
	public void pullImage(String tagImage, String AUTH_EMAIL, String AUTH_USERNAME, String AUTH_PASSWORD) throws DockerException, InterruptedException
	{
		this.docker.pull(tagImage, RegistrationAuth.getAuth(AUTH_EMAIL, AUTH_USERNAME, AUTH_PASSWORD));
	}
	
	public void pushImage(String tagImage, String AUTH_EMAIL, String AUTH_USERNAME, String AUTH_PASSWORD) throws DockerException, InterruptedException
	{
		this.docker.push(tagImage, RegistrationAuth.getAuth(AUTH_EMAIL, AUTH_USERNAME, AUTH_PASSWORD));
	}
	
	public void createImage(String pathDockerFile, String nameImage) throws DockerException, InterruptedException, IOException
	{
		if (!existImage(nameImage))
		{		
			this.docker.build(Paths.get(pathDockerFile), nameImage);
		}
	}
	
	public void createImage(String pathDockerFile, String nameImage, String buildargs) throws DockerException, InterruptedException, IOException
	{
		if (!existImage(nameImage))
		{						
			BuildParam buildParam = BuildParam.create("buildargs", URLEncoder.encode(buildargs, "UTF-8"));
			
			this.docker.build(Paths.get(pathDockerFile), nameImage, buildParam);
		}
	}
		
	public void deleteAllImage() throws DockerException, InterruptedException
	{
		
		List<Image> images = this.docker.listImages(ListImagesParam.allImages(false));
		
		for (Image i : images)
		{
			this.docker.removeImage(i.id());
		}

	}
	
	/*
	 *	METHODS FOR CONTAINERS
	 */
	
	public boolean existContainer(String nameContainer, boolean containerRunning) throws DockerException, InterruptedException
	{
		List<Container> containers = this.docker.listContainers(ListContainersParam.allContainers(!containerRunning));
		
		if (containers != null)
		{
			for (Container c : containers)
			{
				
				ImmutableList<String> names = c.names();
				for (String name : names)
				{
					if (name.length() == 0)
					{
						return false;
					}
					
					if (name.substring(1).equals(nameContainer))
					{
						return true;
					}
				}
			}
		}
		return false;
	}
	
	public String createContainer(String[] ports, String nameContainer, String nameImage) throws DockerException, InterruptedException
	{
	
		if (!this.existContainer(nameContainer, false))
		{
			final Map<String, List<PortBinding>> portBindings = getPorBinding(ports);
			
			final HostConfig hostConfig = HostConfig.builder()
				.portBindings(portBindings).build();

			return createContainer(ports, nameContainer, nameImage, hostConfig);
		}
		
		return null;
		
	}
	
	public String createContainer(String[] ports, String nameContainer, String nameImage, String pathHostMount, String pathContainerMount) throws DockerException, InterruptedException
	{
		
		if (!this.existContainer(nameContainer, false))
		{
			final Map<String, List<PortBinding>> portBindings = getPorBinding(ports);
			
			final HostConfig hostConfig = HostConfig.builder()
				.portBindings(portBindings).appendBinds(Bind.from(pathHostMount).to(pathContainerMount).readOnly(false).build()).build();
			
			return createContainer(ports, nameContainer, nameImage, hostConfig);
		}
		
		return null;
		
	}
	
	public String createContainer(String[] ports, String nameContainer, String nameImage, String pathHostMount, String pathContainerMount, boolean readOnly) throws DockerException, InterruptedException
	{
		
		if (!this.existContainer(nameContainer, false))
		{
			final Map<String, List<PortBinding>> portBindings = getPorBinding(ports);

			final HostConfig hostConfig = HostConfig.builder()
				.portBindings(portBindings).appendBinds(Bind.from(pathHostMount).to(pathContainerMount).readOnly(readOnly).build()).build();
			
			return createContainer(ports, nameContainer, nameImage, hostConfig);
		}
		
		return null;
	}
	
	private Map<String, List<PortBinding>> getPorBinding(String[] ports)
	{
		final Map<String, List<PortBinding>> portBindings = new HashMap<>();
		for (String port : ports) {
		    List<PortBinding> hostPorts = new ArrayList<>();
		    hostPorts.add(PortBinding.of("0.0.0.0", port));
		    portBindings.put(port, hostPorts);
		}
		
		return portBindings;
	}
	
	private String createContainer(String[] ports, String nameContainer, String nameImage, HostConfig hostConfig) throws DockerException, InterruptedException
	{
		final ContainerConfig containerConfig = ContainerConfig.builder()
				.hostConfig(hostConfig).image(nameImage).exposedPorts(ports)
				.tty(true).build();

		final ContainerCreation creation = this.docker.createContainer( containerConfig, nameContainer);
		return creation.id();
	}
	
	public void startContainer(String id) throws DockerException, InterruptedException
	{
		if (id != null)
		{
			this.docker.startContainer(id);
		}
		else
		{
			System.err.println("Error to start container");
		}
	}
	
	public boolean stopContainer(String nameContainer) throws DockerException, InterruptedException
	{
		if (this.existContainer(nameContainer, true))
		{
			this.docker.stopContainer(nameContainer, 20);
			return true;
		}
		
		return false;
	}
	
	public boolean deleteContainer(String nameContainer) throws DockerException, InterruptedException
	{
		boolean stopContainer = this.stopContainer(nameContainer);
		
		if (this.existContainer(nameContainer, false) && stopContainer == true)
		{
			this.docker.removeContainer(nameContainer);
			return true;
		}
		
		return false;
	}
	
	public void deleteAllContainer() throws DockerException, InterruptedException
	{
		List<Container> containers = this.docker.listContainers(ListContainersParam.allContainers(false));
		
		for (Container c : containers)
		{
			this.docker.stopContainer(c.id(), 20);
		}
		
		containers = this.docker.listContainers(ListContainersParam.allContainers(true));
		
		for (Container c : containers)
		{
			this.docker.removeContainer(c.id());
		}
	}
	
	public void addFileToContainer(String pathFile, String containerId, String pathFromContainer) throws DockerCertificateException, DockerException, InterruptedException, IOException
	{
		
		InputStream targetStream = new FileInputStream(new File(pathFile));
        
    	this.docker.copyToContainer(targetStream, containerId, pathFromContainer);
    
	}
	
	/* OTHERS METHODS */
	
	
	public String getIpAddress(String nameContainer) throws DockerException, InterruptedException
	{
		return docker.inspectContainer(nameContainer).networkSettings().ipAddress();
	}
	
	public void commandInContainer(String containerId, String[] command) throws DockerException, InterruptedException, IOException
	{
    	ExecCreation execId = this.docker.execCreate(containerId, command);
    	LogStream stream = this.docker.execStart(execId.id(), DockerClient.ExecStartParameter.TTY);

	}
	
}
