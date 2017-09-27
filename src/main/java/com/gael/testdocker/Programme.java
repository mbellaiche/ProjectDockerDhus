package com.gael.testdocker;

import java.io.OutputStream;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.FileOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileNotFoundException;
import java.util.Properties;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.DockerClient.ExecStartParameter;
import com.spotify.docker.client.DockerClient.LogsParam;
import com.spotify.docker.client.LogStream;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.ExecCreation;

public class Programme {

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
	
    	//String buildargs = "{\"PATHWORKDIR\":\"/home\"}";
    	
    	init();
    	
    	Utils.createTar(nameTar, "lib/");
    	
    	DockerCommands dc = new DockerCommands();
    	
    	createDHuS(dc);
    	
    	dc.close();
    	
    	Utils.deleteFile(nameTar);
    }
    
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
    
    public static void createDHuS(DockerCommands dc) throws DockerException, InterruptedException, DockerCertificateException, IOException
    {
    	createDHuS(dc, "latest");
    }
    
    public static void createDHuS(DockerCommands dc, String versionImage) throws DockerException, InterruptedException, DockerCertificateException, IOException
    {
    	
    	if (versionImage == null || versionImage.equals(""))
    	{
    		versionImage = "latest";
    	}
    	
    	if (!dc.existImage(nameImage))
    	{
    		dc.getDockerClient().pull(nameImage+":"+versionImage);
    	}
    	
    	if (!dc.existContainer(nameContainer, false))
    	{
    		String containerId = createAndStartContainer(dc);
        	dc.addFileToContainer(nameTar, containerId, "/home/dhus/server/lib");
        	launchCommand(dc, containerId);
    	}
    	
    }
    
    public static String createAndStartContainer(DockerCommands dc) throws DockerException, InterruptedException
    {
    	String containerId = dc.createContainer(ports, nameContainer, nameImage);
    	dc.startContainer(containerId);
    	return containerId;
    }
    
    public static void launchCommand(DockerCommands dc, String containerId) throws DockerException, InterruptedException, IOException
    {
    	String[] command = {"bash", "-c", "/root/script.sh"};
    	dc.commandInContainer(containerId, command);
    }
}
