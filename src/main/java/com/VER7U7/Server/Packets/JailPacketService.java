package com.VER7U7.Server.Packets;

import com.VER7U7.Server.JailPools;
import com.VER7U7.Server.JailServer;
import com.VER7U7.Server.Network.NetworkEngine;
import com.VER7U7.Server.Network.NetworkPacket;
import com.VER7U7.Server.Network.States.NetworkPlayerSession;
import com.VER7U7.UnityPhysics.JUPP.JUPPController;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class JailPacketService {

    public Map<Integer, PacketFactory> packetFactoryPool;

    public JailPacketService(JailServer jailServer, JUPPController physicsController, NetworkEngine networkEngine, JailPools jailPools) {
        List<Class<?>> classes = findAllClassesImplementingInterface("com.VER7U7.Server.Packets", PacketFactory.class);

        if (classes == null)
            throw new RuntimeException("Classes with interface PacketFactory not found.");

        List<PacketFactory> packets = new ArrayList<>();
        for (Class<?> clazzFor : classes) {
            try {
                Class<? extends PacketFactory> clazz = clazzFor.asSubclass(PacketFactory.class);
                PacketFactory packetFactory = clazz.getDeclaredConstructor().newInstance();
                packets.add(packetFactory);
            } catch(Exception e) { e.printStackTrace(); }
        }

        packetFactoryPool = new LinkedHashMap<>();
        for (PacketFactory packetFactory : packets) {
            int packetID = packetFactory.initialize(jailServer, physicsController, networkEngine, jailPools);

            if (packetID > 0)
                packetFactoryPool.put(packetID, packetFactory);
        }
    }

    public boolean callToPacketFactory(int packetID, int playerID, NetworkPacket networkPacket) {
        PacketFactory packetFactory = packetFactoryPool.get(packetID);

        if (packetFactory == null)
            return false;

        try {
            packetFactory.process(playerID, networkPacket);
        } catch(Exception e) { e.printStackTrace();}
        return true;
    }

    public static List<Class<?>> findAllClassesImplementingInterface(String packageName, Class<?> interfaceClass) {
        List<Class<?>> classes = new ArrayList<>();
        try {
            String path = packageName.replace(".", "/");
            Enumeration<URL> resources = Thread.currentThread().getContextClassLoader().getResources(path);

            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();

                if (resource.getProtocol().equals("jar")) {
                    classes.addAll(findClassesInJar(resource, packageName));
                } else {
                    File directory = new File(URLDecoder.decode(resource.getFile(), "UTF-8"));
                    classes.addAll(findClasses(directory, packageName));
                }
            }

            List<Class<?>> implementingClasses = new ArrayList<>();
            for (Class<?> clazz : classes) {
                if (interfaceClass.isAssignableFrom(clazz) && !clazz.isInterface() && !clazz.isAnonymousClass()) {
                    implementingClasses.add(clazz);
                }
            }
            return implementingClasses;
        }catch(IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static List<Class<?>> findClassesInJar(URL resource, String packageName) throws IOException, ClassNotFoundException {
        List<Class<?>> classes = new ArrayList<>();
        JarURLConnection jarURLConnection = (JarURLConnection) resource.openConnection();
        JarFile jarFile = jarURLConnection.getJarFile();

        String packagePath = packageName.replace('.', '/');
        Enumeration<JarEntry> entries = jarFile.entries();

        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String entryName = entry.getName();

            if (entryName.startsWith(packagePath) && entryName.endsWith(".class")) {
                String className = entryName.replace('/', '.').substring(0, entryName.length() - 6);
                classes.add(Class.forName(className));
            }
        }

        return classes;
    }

    private static List<Class<?>> findClasses(File directory, String packageName) throws ClassNotFoundException {
        List<Class<?>> classes = new ArrayList<>();
        if (!directory.exists()) {
            return classes;
        }
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    classes.addAll(findClasses(file, packageName + "." + file.getName()));
                } else if (file.getName().endsWith(".class")) {
                    String className = packageName + '.' + file.getName().substring(0, file.getName().length() - 6);
                    classes.add(Class.forName(className));
                }
            }
        }
        return classes;
    }
}

