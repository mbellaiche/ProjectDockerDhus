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

/**
 * <b>Class with methods for commands Docker</b>
 * 
 * <p>Actions available :
 * <ul>
 * <li>Push or pull an image</li>
 * <li>Creation of Images (by pull or Dockerfile)</li>
 * <li>Creation of Containers</li>
 * <li>Management of Containers : start, stop, add files or execute commands bash</li>
 * </ul>
 * </p>
 * 
 * @see DockerClient
 * 
 * @author mbellaiche
 * @version 1.0
 *
 */
public class DockerCommands {
	
	/**
	 * Instance of DockerClient
	 * 
	 * @see DockerCommands#getDockerClient()
	 */
	private DockerClient docker;
	
	/**
	 * Constructor DockerCommands
	 * <p>
	 * initialization of instance DockerClient
	 * </p>
	 * 
	 * @param docker Object DockerClient for using of Docker
	 * 
	 * @see DockerCommands#docker
	 * 				
	 */
	public DockerCommands(DockerClient docker)
	{
		this.docker = docker;
	}
	
	/**
	 * Constructor DockerCommands
	 * <p>
	 * initialization of instance DockerClient with default value
	 * </p>
	 * 
	 * @throws DockerCertificateException
	 * 
 	 * @see DockerCommands#docker
 	 * 
	 */
	public DockerCommands() throws DockerCertificateException
	{
		this(DefaultDockerClient.fromEnv().build());
	}
	
	/**
	 * Returns instance of DockerClient
	 * 
	 * @return Instance of DockerClient to use it for outside
	 * 
	 * @see DockerClient
	 */
	public DockerClient getDockerClient()
	{
		return this.docker;
	}
	
	/**
	 * Close the connection with Docker service
	 */
	public void close()
	{
		this.docker.close();
	}
	
	/*
	 *	METHODS FOR IMAGES
	 */
	
	/**
	 * Returns True if an Image has the name 'nameImage', False otherwise
	 * 
	 * @param nameImage name to check
	 * @return True if 'nameImage' is already used
	 * @throws DockerException Raise if there is error with API
	 * @throws InterruptedException Raise if Thread is interrupted
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
	 * Pull an Image from Registry
	 * 
	 * @param tagImage Name of image to pull
	 * @throws DockerException Raise if there is error with API
	 * @throws InterruptedException Raise if Thread is interrupted
	 */
	public void pullImage(String tagImage) throws DockerException, InterruptedException
	{
		this.docker.pull(tagImage);
	}
	
	/**
	 * Push an Image to Registry
	 * 
	 * @param tagImage Name of image to push
	 * @throws DockerException Raise if there is error with API
	 * @throws InterruptedException Raise if Thread is interrupted
	 */
	public void pushImage(String tagImage) throws DockerException, InterruptedException
	{
		this.docker.push(tagImage);
	}
	
	/**
	 * Pull an Image from private Registry
	 * 
	 * @param tagImage Name of image to pull
	 * @param AUTH_EMAIL Email of registry
	 * @param AUTH_USERNAME Username of registry
	 * @param AUTH_PASSWORD Password of registry
	 * @throws DockerException Raise if there is error with API
	 * @throws InterruptedException Raise if Thread is interrupted
	 */
	public void pullImage(String tagImage, String AUTH_EMAIL, String AUTH_USERNAME, String AUTH_PASSWORD) throws DockerException, InterruptedException
	{
		this.docker.pull(tagImage, RegistrationAuth.getAuth(AUTH_EMAIL, AUTH_USERNAME, AUTH_PASSWORD));
	}
	
	/**
	 * Push an Image to private Registry
	 *
	 * @param tagImage Name of image to push
	 * @param AUTH_EMAIL Email of registry
	 * @param AUTH_USERNAME Username of registry
	 * @param AUTH_PASSWORD Password of registry
	 * @throws DockerException Raise if there is error with API
	 * @throws InterruptedException Raise if Thread is interrupted
	 */
	public void pushImage(String tagImage, String AUTH_EMAIL, String AUTH_USERNAME, String AUTH_PASSWORD) throws DockerException, InterruptedException
	{
		this.docker.push(tagImage, RegistrationAuth.getAuth(AUTH_EMAIL, AUTH_USERNAME, AUTH_PASSWORD));
	}
	
	/**
	 * Create an Image with Dockerfile and a name
	 * 
	 * @param pathDockerFile Path of Dockerfile
	 * @param nameImage Name to use for new Image
	 * @throws DockerException Raise if there is error with API
	 * @throws InterruptedException Raise if Thread is interrupted
	 * @throws IOException Raise if there is an error with Input/Output stream
	 */
	public void createImage(String pathDockerFile, String nameImage) throws DockerException, InterruptedException, IOException
	{
		if (!existImage(nameImage))
		{		
			this.docker.build(Paths.get(pathDockerFile), nameImage);
		}
	}
	
	/**
	 * Create an Image with Dockerfile, arguments and a name

	 * @param pathDockerFile Path of Dockerfile
	 * @param nameImage Name to use for new Image
	 * @param buildargs Arguments for Dockerfile
	 * @throws DockerException Raise if there is error with API
	 * @throws InterruptedException Raise if Thread is interrupted
	 * @throws IOException Raise if there is an error with Input/Output stream
	 */
	public void createImage(String pathDockerFile, String nameImage, String buildargs) throws DockerException, InterruptedException, IOException
	{
		if (!existImage(nameImage))
		{						
			BuildParam buildParam = BuildParam.create("buildargs", URLEncoder.encode(buildargs, "UTF-8"));
			
			this.docker.build(Paths.get(pathDockerFile), nameImage, buildParam);
		}
	}
	
	/**
	 * Delete all images
	 * 
	 * @throws DockerException Raise if there is error with API
	 * @throws InterruptedException Raise if Thread is interrupted
	 */
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
	 * Returns True if an Image has the name 'nameImage', False otherwise
	 * 
	 * @param nameContainer name to check
	 * @param containerRunning True to check containers (running and no-running Containers), false for only no-running Containers 
	 * @return True if 'nameContainer' is already used
	 * @throws DockerException Raise if there is error with API
	 * @throws InterruptedException Raise if Thread is interrupted
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
	 * Create a Container with ports binding
	 * 
	 * @param ports Port to bind with the host
	 * @param nameContainer Name of container to create
	 * @param nameImage Name of image to use for creation of container
	 * @return Id of container created
	 * @throws DockerException Raise if there is error with API
	 * @throws InterruptedException Raise if Thread is interrupted
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
	 * Create a Container with ports binding and mount
	 * 
	 * @param ports Port to bind with the host
	 * @param nameContainer Name of container to create
	 * @param nameImage Name of image to use for creation of container
	 * @param pathHostMount Path from Host to link with the path of container
	 * @param pathContainerMount Path from Container to link with 'pathHostMount'
	 * @return Id of container created
	 * @throws DockerException Raise if there is error with API
	 * @throws InterruptedException Raise if Thread is interrupted
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
	 * Create a Container with ports binding and mount
	 * 
	 * @param ports Port to bind with the host
	 * @param nameContainer Name of container to create
	 * @param nameImage Name of image to use for creation of container
	 * @param pathHostMount Path from Host to link with the path of container
	 * @param pathContainerMount Path from Container to link with 'pathHostMount'
	 * @param readOnly True means mount is readonly
	 * @return Id of container created
	 * @throws DockerException Raise if there is error with API
	 * @throws InterruptedException Raise if Thread is interrupted
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
	 * Returns a map for Ports Bindig
	 * 
	 * @param ports List of String with ports to Bind with host
	 * @return Map of ports for binding
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
	 * Creation of Container with host Configuration
	 * 
	 * @param ports List of ports to bind with Host
	 * @param nameContainer Name of Container to create
	 * @param nameImage name of Image to use for creation of Container
	 * @param hostConfig Configuration of Host for the creation of Container
	 * @return Id of Container created
	 * @throws DockerException Raise if there is error with API
	 * @throws InterruptedException Raise if Thread is interrupted
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
	 * Starts a Container
	 * 
	 * @param id Id of Container to start
	 * @throws DockerException Raise if there is error with API
	 * @throws InterruptedException Raise if Thread is interrupted
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
	 * Stops a Container
	 * 
	 * @param nameContainer Name of Container to stop
	 * @return True if stopped, False otherwise
	 * @throws DockerException Raise if there is error with API
	 * @throws InterruptedException Raise if Thread is interrupted
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
	 * Deletes a Container 
	 * 
	 * @param nameContainer Name of Container to delete
	 * @return True if deleted, False otherwise
	 * @throws DockerException Raise if there is error with API
	 * @throws InterruptedException Raise if Thread is interrupted
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
	
	/**
	 * Delete all container on host
	 * 
	 * @throws DockerException Raise if there is error with API
	 * @throws InterruptedException Raise if Thread is interrupted
	 */
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
	
	/**
	 * Add files into container
	 * 
	 * @param pathFile File to add into Container
	 * @param containerId Id of Container to use
	 * @param pathFromContainer Path from Container to add file
	 * @throws DockerCertificateException Raise if there is an error about Certification
	 * @throws DockerException Raise if there is error with API
	 * @throws InterruptedException Raise if Thread is interrupted
	 * @throws IOException Raise if there is an error with Input/Output stream
	 */
	public void addFileToContainer(String pathFile, String containerId, String pathFromContainer) throws DockerCertificateException, DockerException, InterruptedException, IOException
	{
		
		InputStream targetStream = new FileInputStream(new File(pathFile));
        
    	this.docker.copyToContainer(targetStream, containerId, pathFromContainer);
    
	}
	
	/* OTHERS METHODS */
	
	/**
	 * Returns Ip of Container with the name
	 * 
	 * @param nameContainer
	 * @return Value of Ip
	 * @throws DockerException Raise if there is error with API
	 * @throws InterruptedException Raise if Thread is interrupted
	 */
	public String getIpAddress(String nameContainer) throws DockerException, InterruptedException
	{
		return docker.inspectContainer(nameContainer).networkSettings().ipAddress();
	}
	
	/**
	 * Launch commands in Container 
	 * 
	 * @param containerId Id of Container
	 * @param command List of commands to execute inside the Container
	 * @throws DockerException Raise if there is error with API
	 * @throws InterruptedException Raise if Thread is interrupted
	 * @throws IOException Raise if there is an error with Input/Output stream
	 */
	public void commandInContainer(String containerId, String[] command) throws DockerException, InterruptedException, IOException
	{
    	ExecCreation execId = this.docker.execCreate(containerId, command);
    	LogStream stream = this.docker.execStart(execId.id(), DockerClient.ExecStartParameter.TTY);
	}
	
}
