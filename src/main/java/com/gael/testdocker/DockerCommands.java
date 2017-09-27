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

public class DockerCommands {
	
	/**
	 * DockerClient instance, to communicate with Docker service
	 */
	private DockerClient docker;
	
	/**
	 * Constructor of Class
	 * 
	 * @param docker DockerClient instance
	 */
	public DockerCommands(DockerClient docker)
	{
		this.docker = docker;
	}
	
	/**
	 * Constructor of Class with DockerClient instance with default value
	 * 
	 * @throws DockerCertificateException SubClass of Exception for Docker Certificate
	 */
	public DockerCommands() throws DockerCertificateException
	{
		this(DefaultDockerClient.fromEnv().build());
	}
	
	/**
	 * Returns the DockerClient instance
	 * 
	 * @return DockerClient instance
	 */
	public DockerClient getDockerClient()
	{
		return this.docker;
	}
	
	/**
	 * Close the DockerClient
	 */
	public void close()
	{
		this.docker.close();
	}
	
	/*
	 *	METHODS FOR IMAGES
	 */
	
	/**
	 * Checks if an Image with name 'nameImage' exists
	 * 
	 * @param nameImage name of Image for check
	 * @return return true if there is an Image with the same name, false otherwise
	 * @throws DockerException SubClass of Exception
	 * @throws InterruptedException Thrown when a thread is waiting, sleeping, or otherwise occupied, and the thread is interrupted, either before or during the activity
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
	
	/**
	 * Create an Image, based on Dockerfile, with a name
	 * 
	 * @param pathDockerfile path of Dockerfile
	 * @param nameImage name for Image
	 * @return return Id of Image created, null otherwise
	 * @throws DockerException SubClass of Exception
	 * @throws InterruptedException Thrown when a thread is waiting, sleeping, or otherwise occupied, and the thread is interrupted, either before or during the activity
	 * @throws IOException Subclass of Exception for Input-Output
	 */
	public void createImage(String pathDockerFile, String nameImage) throws DockerException, InterruptedException, IOException
	{
		if (!existImage(nameImage))
		{		
			this.docker.build(Paths.get(pathDockerFile), nameImage);
		}
	}
	
	/**
	 * Create an Image, based on Dockerfile, with a name
	 * 
	 * @param pathDockerfile path of Dockerfile
	 * @param nameImage name for Image
	 * @param buildargs params for Dockerfile
	 * @return return Id of Image created, null otherwise
	 * @throws DockerException SubClass of Exception
	 * @throws InterruptedException Thrown when a thread is waiting, sleeping, or otherwise occupied, and the thread is interrupted, either before or during the activity
	 * @throws IOException Subclass of Exception for Input-Output
	 */
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
	
	/**
	 * Checks if a Container with name 'nameImage' exists
	 * 
	 * @param nameContainer name of Container for check
	 * @param containerRunning boolean to check only Running Containers or all Containers
	 * @return - return true if there is an Container with the same name, false otherwise
	 * @throws DockerException SubClass of Exception
	 * @throws InterruptedException Thrown when a thread is waiting, sleeping, or otherwise occupied, and the thread is interrupted, either before or during the activity
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
	
	/**
	 * Create a container with port from the name of an Image
	 * 
	 * @param ports port binded for Container
	 * @param nameContainer name for Container
	 * @param nameImage name of Image for Container creation
	 * @return id of Container created
	 * @throws DockerException SubClass of Exception
	 * @throws InterruptedException Thrown when a thread is waiting, sleeping, or otherwise occupied, and the thread is interrupted, either before or during the activity
	 */
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
	
	/**
	 * Create a container with port from the name of an Image
	 * 
	 * @param ports port binded for Container
	 * @param nameContainer name for Container
	 * @param nameImage name of Image for Container creation
	 * @param pathHostMount Path of mount of Host
	 * @param pathContainerMount Path of mount inside Container
	 * @return id of Container created
	 * @throws DockerException SubClass of Exception
	 * @throws InterruptedException Thrown when a thread is waiting, sleeping, or otherwise occupied, and the thread is interrupted, either before or during the activity
	 */
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
	
	/**
	 * Create a container with port from the name of an Image
	 * 
	 * @param ports port binded for Container
	 * @param nameContainer name for Container
	 * @param nameImage name of Image for Container creation
	 * @param pathHostMount Path of mount of Host
	 * @param pathContainerMount Path of mount inside Container
	 * @param readOnly true if mount is read only, false otherwise 
	 * @return id of Container created
	 * @throws DockerException SubClass of Exception
	 * @throws InterruptedException Thrown when a thread is waiting, sleeping, or otherwise occupied, and the thread is interrupted, either before or during the activity
	 */
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
	
	/**
	 * Create a map for binding
	 * 
	 * @param ports array of ports for binding
	 * @return Map of Binding ports
	 */
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
	
	/**
	 * Create a container and return Id
	 * 
	 * @param ports Map of ports for binding
	 * @param nameContainer name for created container 
	 * @param nameImage name of image which container is based
	 * @param hostConfig configuration of host for Container
	 * @return id of created container
	 * @throws DockerException SubClass of Exception
	 * @throws InterruptedException Thrown when a thread is waiting, sleeping, or otherwise occupied, and the thread is interrupted, either before or during the activity
	 */
	private String createContainer(String[] ports, String nameContainer, String nameImage, HostConfig hostConfig) throws DockerException, InterruptedException
	{
		final ContainerConfig containerConfig = ContainerConfig.builder()
				.hostConfig(hostConfig).image(nameImage).exposedPorts(ports)
				.tty(true).build();

		final ContainerCreation creation = this.docker.createContainer( containerConfig, nameContainer);
		return creation.id();
	}
	
	/**
	 * Start a container from id
	 * 
	 * @param id id of Container to start
	 * @throws DockerException SubClass of Exception
	 * @throws InterruptedException Thrown when a thread is waiting, sleeping, or otherwise occupied, and the thread is interrupted, either before or during the activity
	 */
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
	
	/**
	 * Stop a container if it exists
	 * 
	 * @param nameContainer name of Container to stop
	 * @return return true if container is stopped, false otherwise
	 * @throws DockerException SubClass of Exception
	 * @throws InterruptedException Thrown when a thread is waiting, sleeping, or otherwise occupied, and the thread is interrupted, either before or during the activity
	 */
	public boolean stopContainer(String nameContainer) throws DockerException, InterruptedException
	{
		if (this.existContainer(nameContainer, true))
		{
			this.docker.stopContainer(nameContainer, 20);
			return true;
		}
		
		return false;
	}
	
	/**
	 * Delete of a Container with the name
	 * 
	 * @param nameContainer name of container to delete
	 * @return return true if container is deleted, false otherwise
	 * @throws DockerException SubClass of Exception
	 * @throws InterruptedException Thrown when a thread is waiting, sleeping, or otherwise occupied, and the thread is interrupted, either before or during the activity
	 */
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
        
    	DockerClient docker = DefaultDockerClient.fromEnv().build();
    	docker.copyToContainer(targetStream, containerId, pathFromContainer);
    
    	docker.close();
	}
	
	/* CREATE IMAGES AND CONTAINERS */
	
	/**
	 * Create an Image and Container from Image
	 * 
	 * @param ports port binded for Container
	 * @param pathDockerFile path of Dockerfile
	 * @param nameImage name of Image for Container creation
	 * @param nameContainer name for Container
	 * @return id of Container created
	 * @throws DockerException SubClass of Exception
	 * @throws InterruptedException Thrown when a thread is waiting, sleeping, or otherwise occupied, and the thread is interrupted, either before or during the activity
	 * @throws IOException Subclass of Exception for Input-Output
	 */
	public String createAndStartContainer(String[] ports, String pathDockerFile, String nameImage, String nameContainer, String buildargs) throws DockerException, InterruptedException, IOException
	{
		if (buildargs == null)
		{
			this.createImage(pathDockerFile, nameImage);
		}
		else
		{
			this.createImage(pathDockerFile, nameImage, buildargs);
		}
	    	
    	String idContainer = this.createContainer(ports, nameContainer, nameImage);
    	this.startContainer(idContainer);
    	
    	return idContainer;
	}
	
	/**
	 * Create an Image and Container from Image
	 * 
	 * @param ports port binded for Container
	 * @param pathDockerFile path of Dockerfile
	 * @param nameImage name of Image for Container creation
	 * @param nameContainer name for Container
	 * @param pathHostMount Path of mount of Host
	 * @param pathContainerMount Path of mount inside Container
	 * @return id of container created
	 * @throws DockerException SubClass of Exception
	 * @throws InterruptedException Thrown when a thread is waiting, sleeping, or otherwise occupied, and the thread is interrupted, either before or during the activity
	 * @throws IOException Subclass of Exception for Input-Output
	 */
	public String createAndStartContainer(String[] ports, String pathDockerFile, String nameImage, String nameContainer, String pathHostMount, String pathContainerMount, String buildargs) throws DockerException, InterruptedException, IOException
	{
		if (buildargs == null)
		{
			this.createImage(pathDockerFile, nameImage);
		}
		else
		{
			this.createImage(pathDockerFile, nameImage, buildargs);
		}

		String idContainer = this.createContainer(ports, nameContainer, nameImage, pathHostMount, pathContainerMount);
    	this.startContainer(idContainer);
    	
    	return idContainer;
	}
	
	/**
	 * Create an Image and Container from Image
	 * 
	 * @param ports port binded for Container
	 * @param pathDockerFile path of Dockerfile
	 * @param nameImage name of Image for Container creation
	 * @param nameContainer name for Container
	 * @param pathHostMount Path of mount of Host
	 * @param pathContainerMount Path of mount inside Container
	 * @param readOnly true if mount is read only, false otherwise 
	 * @return id of container created
	 * @throws DockerException SubClass of Exception
	 * @throws InterruptedException Thrown when a thread is waiting, sleeping, or otherwise occupied, and the thread is interrupted, either before or during the activity
	 * @throws IOException Subclass of Exception for Input-Output
	 */
	public String createAndStartContainer(String[] ports, String pathDockerFile, String nameImage, String nameContainer, String pathHostMount, String pathContainerMount, boolean readOnly, String buildargs) throws DockerException, InterruptedException, IOException
	{
		if (buildargs == null)
		{
			this.createImage(pathDockerFile, nameImage);
		}
		else
		{
			this.createImage(pathDockerFile, nameImage, buildargs);
		}
	    	
		String idContainer = this.createContainer(ports, nameContainer, nameImage, pathHostMount, pathContainerMount, readOnly);
	   	this.startContainer(idContainer);
	    	
	   	return idContainer;
	}
	
	/* OTHERS METHODS */
	
	/**
	 * Returns IP Address of a container from a name
	 * 
	 * @param nameContainer name of container to get Ip Address
	 * @return ip address pf container
	 * @throws DockerException SubClass of Exception
	 * @throws InterruptedException Thrown when a thread is waiting, sleeping, or otherwise occupied, and the thread is interrupted, either before or during the activity
	 */
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