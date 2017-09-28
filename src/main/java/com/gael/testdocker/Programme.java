package com.gael.testdocker;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;

/**
 * 
 * @author bellaiche
 * @version 1.0
 * 
 */
public class Programme {

	/**
	 * Class for Docker commands
	 * 
	 * @see DockerCommands
	 */
	private static DockerCommands dc = null;
	
	private static Properties prop;
	private static String[] ports;
	private static String pathDockerFile;
	private static String nameImage;
	private static String nameContainer;
	private static String portProperties;
	
	private static String nameTar = "lib.tar";
	
	private static String configName = "config.properties";
	private static String pathDockerFileProperty = "pathdockerfile";
	private static String nameImageProperty = "nameImage";
	private static String nameContainerProperty = "nameContainer";
	private static String portsProperty = "ports";

	private static String usernameHub;
	private static String emailHub;
	private static String passwordHub;

	/**
     * Charge la liste des propriétés contenu dans le fichier spécifié
     *
     * @param filename le fichier contenant les propriétés
     * @return un objet Properties contenant les propriétés du fichier
     */
	public static Properties load(String filename) throws IOException, FileNotFoundException{
		Properties properties = new Properties();
		FileInputStream input = new FileInputStream(filename);
		try{
			properties.load(input);
			return properties;
		}
		finally{
			input.close();
		}
		
	}
	
    public static void main(String[] args) throws FileNotFoundException, IOException, DockerCertificateException, DockerException, InterruptedException {
	
    	if (args.length != 3)
    	{
    		throw new IllegalArgumentException("Error ! Programm need 3 environments variables !");
    	}
    	else
    	{
    		emailHub = args[0];
    		usernameHub = args[1];
    		passwordHub = args[2];
    	}
    	
    	//String buildargs = "{\"PATHWORKDIR\":\"/home\"}";
    	
    	init();
    	
    	Utils.createTar(nameTar, "lib/");
    	
    	dc = new DockerCommands();
    	
    	createDHuS(dc);
    	
    	dc.close();
    	
    	Utils.deleteFile(nameTar);
    }
    
    /**
     * Initialize variables
     * 
     * @throws FileNotFoundException Raise if the properties file is not found
	 * @throws IOException Raise if there is an error with Input/Output stream
     */
    private static void init() throws FileNotFoundException, IOException
    {
    	prop = Programme.load(configName);
    	
        ports = null;
    	
    	pathDockerFile = prop.getProperty(pathDockerFileProperty, "./containerRepo");
    	nameImage = prop.getProperty(nameImageProperty, "centosimageref");
    	nameContainer = prop.getProperty(nameContainerProperty, "centoscontainer");
    	
    	portProperties = prop.getProperty(portsProperty, "8081");
    	
    	if (portProperties.contains(";"))
    	{
    		ports = portProperties.split(";");
    	}
    	else
    	{
    		ports = new String[1];
    		ports[0] = portProperties;
    	}
    }
    
    /**
     * Create the latest Image and create the Container
     * 
     * @param dc Instance of DockerCommands for Docker Commands
	 * @throws DockerException Raise if there is error with API
	 * @throws InterruptedException Raise if Thread is interrupted
	 * @throws DockerCertificateException Raise if there is an error about Certification
	 * @throws IOException Raise if there is an error with Input/Output stream
	 * 
	 * @see DockerCommands
	 * 
     */
    public static void createDHuS(DockerCommands dc) throws DockerException, InterruptedException, DockerCertificateException, IOException
    {
    	createDHuS(dc, "latest");
    }
    
    /**
     * Create a version of the Image and create the Container
     * 
     * @param dc Instance of DockerCommands for Docker Commands
     * @param versionImage Version of the image
	 * @throws DockerException Raise if there is error with API
	 * @throws InterruptedException Raise if Thread is interrupted
	 * @throws DockerCertificateException Raise if there is an error about Certification
	 * @throws IOException Raise if there is an error with Input/Output stream
	 * 
	 * @see DockerCommands
	 * 
     */
    public static void createDHuS(DockerCommands dc, String versionImage) throws DockerException, InterruptedException, DockerCertificateException, IOException
    {
    	
    	if (versionImage == null || versionImage.equals(""))
    	{
    		versionImage = "latest";
    	}
    	
    	if (!dc.existImage(nameImage))
    	{
    		dc.pullImage(usernameHub+"/"+nameImage+":"+versionImage, emailHub, usernameHub, passwordHub);
    	}
    	
    	if (!dc.existContainer(nameContainer, false))
    	{
    		String containerId = createAndStartContainer(dc);
        	dc.addFileToContainer(nameTar, containerId, "/home/dhus/server/lib");
        	launchCommand(dc, containerId);
    	}
    	
    }
    
    /**
     * return Id of created and started Container
     *
     * @param dc Instance of DockerCommands for Docker Commands
     * @return Id of the new container
	 * @throws DockerException Raise if there is error with API
	 * @throws InterruptedException Raise if Thread is interrupted
	 * 
	 * @see DockerCommands
     */
    public static String createAndStartContainer(DockerCommands dc) throws DockerException, InterruptedException
    {
    	String containerId = dc.createContainer(ports, nameContainer, nameImage);
    	dc.startContainer(containerId);
    	return containerId;
    }
    
    /**
     * Launch commands Bash 
     * 
     * @param dc Instance of DockerCommands for Docker Commands
     * @param containerId Id of Container 
	 * @throws DockerException Raise if there is error with API
	 * @throws InterruptedException Raise if Thread is interrupted
	 * @throws IOException Raise if there is an error with Input/Output stream
	 * 
	 * @see DockerCommands
     */
    public static void launchCommand(DockerCommands dc, String containerId) throws DockerException, InterruptedException, IOException
    {
    	String[] command = {"bash", "-c", "/root/script.sh"};
    	dc.commandInContainer(containerId, command);
    }
}
