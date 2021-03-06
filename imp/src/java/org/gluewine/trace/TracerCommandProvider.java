/**************************************************************************
 *
 * Gluewine Trace Module
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
package org.gluewine.trace;

import java.util.ArrayList;
import java.util.List;

import org.gluewine.console.CLICommand;
import org.gluewine.console.CommandContext;
import org.gluewine.console.CommandProvider;
import org.gluewine.core.Glue;
import org.gluewine.core.Repository;

/**
 * CommandProvider that allows to interactively start and stop the tracer.
 *
 * @author fks/Serge de Schaetzen
 *
 */
public class TracerCommandProvider implements CommandProvider
{
    // ===========================================================================
    /**
     * The repository to register with.
     */
    @Glue
    private Repository repos;

    /**
     * The tracer instance.
     */
    private Tracer tracer = null;

    // ===========================================================================
    @Override
    public List<CLICommand> getCommands()
    {
        List<CLICommand> cmds = new ArrayList<CLICommand>();

        CLICommand cmd = new CLICommand("trace_on", "Enables tracing.");
        cmd.addOption("-log", "Traces to the log", false, false);
        cmd.addOption("-xml", "Traces to xml file", false, false);
        cmd.addOption("-file", "xml file name", false, true);
        cmds.add(cmd);
        cmds.add(new CLICommand("trace_off", "Disables tracing."));

        return cmds;
    }

    // ===========================================================================
    /**
     * Enables tracing.
     *
     * @param cc The current context.
     */
    public synchronized void _trace_on(CommandContext cc)
    {
        if (tracer == null)
        {
            if (cc.hasOption("-log")) tracer = new LogTracer();
            else if (cc.hasOption("-xml"))
            {
                if (cc.hasOption("-file"))
                {
                    tracer = new XMLTracer(cc.getOption("-file"));
                }
                else cc.println("You must specify a file name (using -file) when using the -xml option.");
            }
            else cc.println("You must either specify -xml or -log!");

            if (tracer != null) repos.register(tracer);
        }
        else cc.println("Tracing is already active!");
    }

    // ===========================================================================
    /**
     * Disables tracing.
     *
     * @param cc The current context.
     */
    public synchronized void _trace_off(CommandContext cc)
    {
        if (tracer != null)
        {
            repos.unregister(tracer);
            tracer.close();
            tracer = null;
        }
        else cc.println("Tracing is not active!");
    }
}
