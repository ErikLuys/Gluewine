/**************************************************************************
 *
 * Gluewine Console Module
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
package org.gluewine.console.impl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

import org.gluewine.authentication.Authenticator;
import org.gluewine.console.CLICommand;
import org.gluewine.console.CLIOption;
import org.gluewine.console.CommandContext;
import org.gluewine.console.CommandProvider;
import org.gluewine.console.ConsoleServer;
import org.gluewine.console.FailedResponse;
import org.gluewine.console.Request;
import org.gluewine.console.Response;
import org.gluewine.console.SyntaxException;
import org.gluewine.console.SyntaxResponse;
import org.gluewine.core.Glue;
import org.gluewine.core.Repository;
import org.gluewine.core.RepositoryListener;
import org.gluewine.core.RunOnActivate;
import org.gluewine.sessions.Unsecured;

/**
 * Default implementation of ConsoleServer.
 *
 * @author fks/Serge de Schaetzen
 *
 */
public class ConsoleServerImpl implements ConsoleServer, CommandProvider,  RepositoryListener<CommandProvider>
{
    // ===========================================================================
    /**
     * Map of providers indexed on the commands they provide.
     */
    private Map<String, CommandProvider> providers = new HashMap<String, CommandProvider>();

    /**
     * The map of authenticator classes indexed on their user friendly name.
     */
    private Map<String, String> authenticators = new HashMap<String, String>();

    /**
     * The map of available commands.
     */
    private Map<String, CLICommand> commands = new TreeMap<String, CLICommand>();

    /**
     * The object repository.
     */
    @Glue
    private Repository repository = null;

    /**
     * The welcome message to use.
     */
    private String welcomeMessage = null;

    /**
     * The prompt to use.
     */
    private String prompt = null;

    /**
     * The property file to use.
     */
    @Glue(properties = "console.properties", refresh = "propertiesChanged")
    private Properties props;

    // ===========================================================================
    /**
     * Creates an instance.
     */
    public ConsoleServerImpl()
    {
        providers.put("_help", this);
    }

    // ===========================================================================
    /**
     * Checks for available authenticators and registers them.
     */
    @RunOnActivate
    public void checkAuthenticators()
    {
        repository.addListener(new RepositoryListener<Authenticator>()
        {
            @Override
            public void registered(Authenticator a)
            {
                authenticators.put(a.getAuthenticatorName(), a.getAuthenticatorClassName());
            }

            @Override
            public void unregistered(Authenticator a)
            {
                authenticators.remove(a.getAuthenticatorName());
            }
        });

        propertiesChanged();
    }

    // ===========================================================================
    @Override
    public Response executeCommand(Request req) throws Throwable
    {
        String command = req.getCommand();
        int i = command.indexOf(' ');
        String params = null;
        if (i > 0)
        {
            params = command.substring(i).trim();
            command = command.substring(0, i);
        }

        if (providers.containsKey(command))
        {
            CommandProvider prov = providers.get(command);
            CLICommand cmd = commands.get(command);

            if (cmd.isAlias())
            {
                String alias = cmd.getAlias();
                i = command.indexOf(' ');
                if (i > 0)
                {
                    params = params + alias.substring(i);
                    command = alias.substring(0, i);
                }
                else command = alias;
            }

            BufferedCommandInterpreter ci = new BufferedCommandInterpreter(params, req.isOutputRouted(), req.isBatch(), req.isInteractive());
            Response response = null;
            try
            {
                Set<CLIOption> options = cmd.getOptions();
                if (!options.isEmpty())
                {
                    StringBuilder syntax = new StringBuilder(cmd.getName());
                    Map<String, boolean[]> opts = new HashMap<String, boolean[]>();
                    for (CLIOption opt : options)
                    {
                        boolean[] bool = new boolean[] {opt.isRequired(), opt.needsValue()};
                        opts.put(opt.getName(), bool);

                        syntax.append(" ");
                        if (!opt.isRequired()) syntax.append("[");
                        syntax.append(opt.getName());

                        if (opt.needsValue())
                            syntax.append(" <").append(opt.getDescription()).append(">");

                        if (!opt.isRequired()) syntax.append("]");
                    }

                    ci.parseOptions(opts, syntax.toString());
                }

                Method m = prov.getClass().getMethod("_" + command, CommandContext.class);
                m.invoke(prov, new Object[] {ci});
                response = new Response(ci.getOutput(), req.isOutputRouted(), req.isBatch(), req.isInteractive());
            }
            catch (SyntaxException e)
            {
                response = new SyntaxResponse(e.getMessage(), cmd, req.isOutputRouted(), req.isBatch(), req.isInteractive());
            }
            catch (Throwable e)
            {
                System.out.println("Other Exception");
                if (e instanceof InvocationTargetException)
                    e = ((InvocationTargetException) e).getCause();

                response = new FailedResponse(e, req.isOutputRouted(), req.isBatch(), req.isInteractive());
            }
            return response;
        }
        else return new Response("Unknown command !", req.isOutputRouted(), req.isBatch(), req.isInteractive());
    }

    // ===========================================================================
    @Override
    public void registered(CommandProvider t)
    {
        for (CLICommand cmd : t.getCommands())
        {
            commands.put(cmd.getName(), cmd);
            providers.put(cmd.getName(), t);
        }
    }

    // ===========================================================================
    @Override
    public void unregistered(CommandProvider t)
    {
        for (CLICommand cmd : t.getCommands())
        {
            providers.remove(cmd.getName());
            commands.remove(cmd.getName());
        }
    }

    // ===========================================================================
    /**
     * Executes the help command.
     *
     * @param ci The current context.
     */
    public void _help(CommandContext ci)
    {
        String filter = ci.nextArgument();

        ci.tableHeader("Command", "Options", "Description");
        String prevLetter = null;

        for (Entry<String, CLICommand> e : commands.entrySet())
        {
            if (filter == null || e.getKey().startsWith(filter))
            {
                String firstLetter = e.getKey().substring(0, 1);
                if (prevLetter != null && !prevLetter.equals(firstLetter))
                    ci.tableSeparator();

                boolean firstRow = true;
                for (CLIOption opt : e.getValue().getOptions())
                {
                    StringBuilder b = new StringBuilder();
                    if (!opt.isRequired()) b.append("[");
                    b.append(opt.getName());
                    if (!opt.isRequired()) b.append("]");
                    b.append(" ");
                    if (opt.needsValue()) b.append("<");
                    else b.append(" - ");
                    b.append(opt.getDescription());
                    if (opt.needsValue()) b.append(">");

                    if (firstRow)
                        ci.tableRow(e.getKey(), b.toString(), e.getValue().getDescription());

                    else
                        ci.tableRow("", b.toString(), "");

                    firstRow = false;
                }

                if (firstRow)
                    ci.tableRow(e.getKey(), "", e.getValue().getDescription());

                prevLetter = firstLetter;
            }
        }

        ci.printTable();
    }

    // ===========================================================================
    @Override
    @Unsecured
    public boolean needsAuthentication()
    {
        return !authenticators.isEmpty();
    }

    // ===========================================================================
    @Override
    public List<CLICommand> getCommands()
    {
        List<CLICommand> l = new ArrayList<CLICommand>();

        l.add(new CLICommand("help", "Displays the list of available commands."));

        CLICommand cmd = new CLICommand("h", "Displays the list of available commands.");
        cmd.setAlias("help");
        l.add(cmd);

        return l;
    }

    // ===========================================================================
    @Override
    public List<String> complete(String command)
    {
        List<String> res = new ArrayList<String>();

        String[] cmd = null;
        if (command == null || command.trim().length() == 0) cmd = new String[] {""};
        else cmd = command.split(" ");

        for (String s : providers.keySet())
        {
            if (s.startsWith(cmd[0]))
                res.add(s);
        }

        Collections.sort(res);
        return res;
    }

    // ===========================================================================
    @Override
    @Unsecured
    public Map<String, String> getAvailableAuthenticators()
    {
        Map<String, String> m = new HashMap<String, String>(authenticators.size());
        m.putAll(authenticators);
        return m;
    }

    // ===========================================================================
    @Override
    public String getWelcomeMessage()
    {
        return welcomeMessage;
    }

    // ===========================================================================
    @Override
    public String getPrompt()
    {
        return prompt;
    }

    // ===========================================================================
    /**
     * Invoked when the property file has changed.
     */
    public void propertiesChanged()
    {
        welcomeMessage = props.getProperty("welcome.text", "");
        prompt = props.getProperty("prompt.text", "");
    }
}
