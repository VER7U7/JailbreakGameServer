package com.VER7U7.Server.Packets.Services;

import com.VER7U7.Server.Core.JailServer;
import com.VER7U7.Server.Network.NetworkPacket;
import com.VER7U7.Server.Packets.Handlers.FunctionGlobalArgs;
import com.VER7U7.Server.Packets.Handlers.PacketFunction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static com.VER7U7.Server.Packets.Factory.PacketConstants.*;

public class JailPacketService {
    private static final Logger LOGGER = LogManager.getLogger(JailPacketService.class);

    public Map<IncomingPacketType, PacketFunction> packetFactoryPool;

    public JailPacketService(FunctionGlobalArgs functionGlobalArgs) {
        List<Class<?>> classes = findAllClassesImplementingInterface("com.VER7U7.Server.Packets.Handlers", PacketFunction.class);

        if (classes == null)
            throw new RuntimeException("Classes with interface PacketFactory not found.");

        List<PacketFunction> packets = new ArrayList<>();
        for (Class<?> clazzFor : classes) {
            try {
                Class<? extends PacketFunction> clazz = clazzFor.asSubclass(PacketFunction.class);
                PacketFunction packetFunction = clazz.getDeclaredConstructor().newInstance();
                packets.add(packetFunction);
            } catch(Exception e) { LOGGER.error(e); }
        }

        packetFactoryPool = new LinkedHashMap<>();
        for (PacketFunction packetFunction : packets) {
            IncomingPacketType packetType = packetFunction.initialize(functionGlobalArgs);

            if (packetType.getID() > 0)
                packetFactoryPool.put(packetType, packetFunction);
        }
    }

    public boolean callToPacketFactory(IncomingPacketType packetType, short playerID, NetworkPacket networkPacket) {
        PacketFunction packetFunction = packetFactoryPool.get(packetType);

        if (packetFunction == null)
            return false;

        try {
            packetFunction.process(playerID, networkPacket);
        } catch(Exception e) { LOGGER.error(e); }
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
            LOGGER.error(e);
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

