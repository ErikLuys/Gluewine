/**************************************************************************
 *
 * Gluewine GXO Client Module
 *
 * Copyright (C) 2013 FKS bvba               http://www.fks.be/
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ***************************************************************************/
package org.gluewine.gxo_client;

import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.net.Socket;

import org.apache.log4j.Logger;
import org.gluewine.gxo.CompressedBlockInputStream;
import org.gluewine.gxo.CompressedBlockOutputStream;
import org.gluewine.gxo.ExecBean;
import org.gluewine.gxo.GxoException;
import org.gluewine.gxo.InitBean;
import org.gluewine.gxo.LocalAccess;
import org.gluewine.gxo.ProxyAlias;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.StreamException;
import com.thoughtworks.xstream.io.xml.StaxDriver;

/**
 * The client (GWT) side of the Gxo library.
 *
 * This class connects to the given host and port, and can then be used
 * to obtain proxies to Interfaces hosted on the server.
 *
 * @author fks/Serge de Schaetzen
 *
 */
public class GxoClient
{
    // ===========================================================================
    /**
     * The handler used in the generated proxies. The handler encapsulates every
     * invocation request in an ExecBean that is being sent to the server using
     * XStream.
     */
    private class Handler implements InvocationHandler
    {
        /**
         * The ip address.
         */
        private String ipAddress = null;

        // ===========================================================================
        /**
         * The unqualified service name.
         */
        private String service = null;

        /**
         * The current session id.
         */
        private String sessionId = null;

        // ===========================================================================
        /**
         * Creates an instance.
         *
         * @param name The name of the service.
         * @param sessionid The current session id.
         * @param address The up address of the invoker.
         */
        Handler(String name, String sessionid, String address)
        {
            service = name;
            this.sessionId = sessionid;
            this.ipAddress = address;
        }

        // ===========================================================================
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
        {
            ProxyAlias pa = method.getAnnotation(ProxyAlias.class);
            if (pa != null)
                return getInstantiatable(method.getReturnType(), sessionId, ipAddress, args);

            else
            {
                try
                {
                    ExecBean exec = new ExecBean();
                    exec.setSessionId(sessionId);
                    exec.setService(service);
                    exec.setMethod(method.getName());
                    exec.setIpAddress(ipAddress);

                    exec.setParamTypes(method.getParameterTypes());
                    exec.setParams(args);
                    logger.debug("Sending EXEC request for " + service + ":" + method.getName());
                    return write(exec);
                }
                catch (InvocationTargetException e)
                {
                    throw e.getCause();
                }
            }
        }
    }

    /**
     * Flag indicating that the client is connected.
     */
    private boolean connected = false;

    /**
     * The host name to connect to.
     */
    private String host = null;

    /**
     * The inputstream.
     */
    private InputStreamReader in;

    /**
     * The output stream.
     */
    private OutputStreamWriter out;

    /**
     * The port to connect to.
     */
    private int port = 0;

    /**
     * The socket connected (or not) to the server.
     */
    private Socket socket;

    /**
     * The XStream serializer/deserializer.
     */
    private XStream stream = null;

    /**
     * The logger instance to use.
     */
    private Logger logger = Logger.getLogger(getClass());

    /**
     * Flag that defines that the server is local (ie. in the same JVM).
     */
    private boolean local = false;

    // ===========================================================================
    /**
     * Creates an instance that connects locally.
     */
    public GxoClient()
    {
        this(null, 0);
        local = true;
    }

    // ===========================================================================
    /**
     * Creates an instance.
     *
     * @param host The host to connect to.
     * @param port The server port to connect to.
     */
    public GxoClient(String host, int port)
    {
        this.host = host;
        this.port = port;

        stream = new XStream(new StaxDriver());
    }

    // ===========================================================================
    /**
     * Closes the connection.
     */
    public synchronized void close()
    {
        if (connected)
        {
            if (!local)
            {
                try
                {
                    socket.close();
                }
                catch (Throwable e)
                {
                    logger.warn(e);
                }
                socket = null;
            }
            else
            {
                try
                {
                    LocalAccess.getInstance().close();
                }
                catch (Throwable e)
                {
                    logger.warn(e);
                }
            }
            connected = false;
        }
    }

    // ===========================================================================
    /**
     * Attempts to connect to the server identified by the host and port
     * specified in the constructor.
     *
     * @throws Throwable If the connection failed.
     */
    private void connect() throws Throwable
    {
        synchronized (stream)
        {
            if (!local)
            {
                if (socket != null && socket.isInputShutdown())
                    close();

                if (!connected)
                {
                    socket = new Socket();
                    socket.connect(new InetSocketAddress(host, port), 10000);
                    socket.setSoTimeout(30000);
                    CompressedBlockInputStream cin = new CompressedBlockInputStream(socket.getInputStream());
                    CompressedBlockOutputStream cout = new CompressedBlockOutputStream(socket.getOutputStream(), 1024);
                    in = new InputStreamReader(cin, "UTF-8");
                    out = new OutputStreamWriter(cout, "UTF-8");
                }
            }
            else
            {
                LocalAccess la = LocalAccess.getInstance();
                out = new OutputStreamWriter(la.getServerOutputStream(), "UTF-8");
                in = new InputStreamReader(la.getServerInputStream(), "UTF-8");
            }
            connected = true;
        }
    }

    // ===========================================================================
    /**
     * Creates an instance of the class specified.
     *
     * @param <T> The class to return.
     * @param t The class to instantiate.
     * @param sessionid The current session id.
     * @param ip The ip address of the invoker.
     * @param params The parameters to use during instantiation.
     * @return The proxy to the new instance.
     * @throws Throwable If a problem occurs.
     */
    public <T> T getInstantiatable(Class<T> t, String sessionid, String ip,  Object ... params) throws Throwable
    {
        InitBean init = new InitBean();
        init.setClassName(t.getName());
        if (params != null)
        {
            Class<?>[] cls = new Class<?>[params.length];
            for (int i = 0; i < params.length; i++)
                cls[i] = params[i].getClass();
            init.setParamTypes(cls);
            init.setParamValues(params);
        }

        Object id = write(init);
        if (id != null && id instanceof String)
            return getNamedService(t, (String) id, sessionid, ip);

        else
            throw new Throwable("Could not create an instance of " + t.getName() + ", value returned was " + id);
    }

    // ===========================================================================
    /**
     * Creates and returns a proxy to the service specified by the given name.
     * The session id is used when security is activated (at the server side).
     *
     * @param <T> The class to return.
     * @param t The interface to be proxied.
     * @param name The name the service has been bound with.
     * @param sessionid The current session id.
     * @param ip The ip address of the invoker.
     * @return The Proxy to the interface.
     */
    @SuppressWarnings("unchecked")
    public <T> T getNamedService(Class<T> t, String name, String sessionid, String ip)
    {
        return (T) Proxy.newProxyInstance(t.getClassLoader(), new Class<?>[] {t}, new Handler(name, sessionid, ip));
    }

    // ===========================================================================
    /**
     * Creates and returns a proxy to the service specified by the given interface.
     * The session id is used when security is activated (at the server side).
     *
     * @param <T> The class to return.
     * @param t The interface to be proxied.
     * @param sessionid The current session id.
     * @param ip The ip address of the invoker.
     * @return The Proxy to the interface.
     */
    @SuppressWarnings("unchecked")
    public <T> T getService(Class<T> t, String sessionid, String ip)
    {
        return (T) Proxy.newProxyInstance(t.getClassLoader(), new Class<?>[] {t}, new Handler(t.getName(), sessionid, ip));
    }

    // ===========================================================================
    /**
     * Returns the xstream stream.
     * @return The xstream stream
     */
    public XStream getStream()
    {
        return stream;
    }

    // ===========================================================================
    /**
     * Writes the object specified and returns the answer.
     *
     * @param o The object to write.
     * @return The result.
     * @throws Throwable If the connections failed, or the returned value is
     */
    private Object write(Object o) throws Throwable
    {
        Object result = null;
        int retries = 3;
        synchronized (stream)
        {
            while (true)
            {
                connect();
                if (connected)
                {
                    try
                    {
                        stream.toXML(o, out);
                        out.flush();
                        result = stream.fromXML(in);

                        break;
                    }
                    catch (StreamException e)
                    {
                        close();
                    }
                    catch (Throwable e)
                    {
                        if (!local)
                        {
                            try
                            {
                                socket.close();
                            }
                            catch (Throwable t)
                            {
                                logger.warn(t);
                            }
                            connected = false;
                            throw e;
                        }
                        else
                            LocalAccess.getInstance().close();
                    }
                    retries--;
                    if (retries == 0)
                        throw new GxoException("Could not connect to server");
                }
                else
                    throw new Throwable("Could not connect to server " + host + ":" + port);
            }

            if (result instanceof GxoException)
            {
                if (((Throwable) result).getCause() != null)
                    throw ((Throwable) result).getCause();
                else
                    throw (GxoException) result;
            }

            else if (o instanceof Throwable)
            {
                if (!local)
                {
                    try
                    {
                        socket.close();
                    }
                    catch (Throwable e)
                    {
                        logger.warn(e);
                    }
                    connected = false;
                }
                throw (Throwable) o;
            }
        }

        return result;
    }
}
