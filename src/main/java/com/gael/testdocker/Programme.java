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
	
    public static void main(String[] args) throws DockerCertificateException, IOException, DockerException, InterruptedException {
	
    	String buildargs = "{\"PATHWORKDIR\":\"/home\"}";
    	
//    	try {
//			launchCommandes();
//		} catch (IOException | DockerCertificateException | DockerException | InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
    
    	Properties prop = Programme.load("config.properties");
    	
    	String[] ports = null;
    	
    	String pathDockerFile = prop.getProperty("pathdockerfile", "./containerRepo");
    	String nameImage = prop.getProperty("nameImage", "centosimageref");
    	String nameContainer = prop.getProperty("nameContainer", "centoscontainer");
    	
    	String portProperties = prop.getProperty("ports", "8081");
    	
    	if (portProperties.contains(";"))
    	{
    		ports = portProperties.split(";");
    	}
    	else
    	{
    		ports = new String[1];
    		ports[0] = portProperties;
    	}
    	
    	Utils.createTar("lib.tar", "lib/");
    	
    	DockerCommands dc = new DockerCommands();
    	
    	String containerId = dc.createContainer(ports, nameContainer, nameImage);
    	dc.startContainer(containerId);
    
    	dc.addFileToContainer("lib.tar", containerId, "/home/dhus/server/lib");
    	
    	String[] command = {"bash", "-c", "/root/script.sh"};
    	dc.commandInContainer(containerId, command);
    	
    	dc.close();
    	    	
    }
    
    public static void launchCommandes() throws FileNotFoundException, IOException, DockerCertificateException, DockerException, InterruptedException
    {
    	Properties prop = Programme.load("config.properties");
    	
    	String[] ports = null;
    	
    	String pathDockerFile = prop.getProperty("pathdockerfile", "./containerRepo");
    	String nameImage = prop.getProperty("nameImage", "centosimage");
    	String nameContainer = prop.getProperty("nameContainer", "centoscontainer");
    	
    	String portProperties = prop.getProperty("ports", "8081");
    	
    	if (portProperties.contains(";"))
    	{
    		ports = portProperties.split(";");
    	}
    	else
    	{
    		ports = new String[1];
    		ports[0] = portProperties;
    	}
    	
    	String pathHostMount = prop.getProperty("pathHostMount", "centosimage");
    	String pathContainerMount = prop.getProperty("pathContainerMount", "centoscontainer");
    	
    	DockerCommands dc = new DockerCommands();
    	
    	//dc.deleteAllContainer();
    	String id = dc.createAndStartContainer(ports, pathDockerFile, nameImage, nameContainer, null);
    	
    	String[] command = {"bash", "-c", "/root/script.sh"};
    	dc.commandInContainer(id, command);
    	
    	dc.close();
    }
}
