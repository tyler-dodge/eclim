/**
 * Copyright (C) 2012 Tyler Dodge
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.eclim.plugin.core.command.project;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IProject;

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import java.util.ArrayList;
import java.util.List;

import org.eclim.command.CommandLine;
import org.eclim.command.Options;

import org.eclim.plugin.core.command.AbstractCommand;
import org.eclim.annotation.Command;

import org.eclim.plugin.core.util.ProjectUtils;

import org.eclipse.debug.ui.DebugUITools;

import org.eclipse.debug.ui.IDebugUIConstants;

import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.core.DebugPlugin;

@org.eclim.annotation.Command(
    name = "project_run",
    options = "OPTIONAL l list ANY, OPTIONAL i indices ANY, OPTIONAL d debug ANY, OPTIONAL p project ARG"
    )
public class ProjectRunCommand
  extends AbstractCommand
{
  private static final String FORMAT_SHOW_INDICES = "%-3d: %s\n";
  private static final String FORMAT_NO_INDICES = "%s\n";
  private static final String FORMAT_STARTED = "%s Started";
  
  private static final String ERROR_PREFIX = "Error: ";
  private static final String ERROR_INVALID_ARGS = ERROR_PREFIX + "Invalid Args";
  private static final String ERROR_NOT_IN_RANGE = ERROR_PREFIX + "Index Out of Range";
  private static final String ERROR_MULTIPLE_VAL = ERROR_PREFIX + "Multiple Launch Configurations Found";
  private static final String ERROR_NOT_FOUND    = ERROR_PREFIX + "Configuration Not Found";

  /**
   * {@inheritDoc}
   */
  public String execute(CommandLine commandLine)
    throws Exception
  {
    IProject project = getProject(commandLine);
    List<ILaunchConfiguration> configs = filterByProject(project, DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurations());
    String mode = getMode(commandLine);

    //Lists launchers if l is set
    if (commandLine.hasOption(Options.LIST_OPTION)) {
      boolean showIndices = commandLine.hasOption(Options.INDEXED_OPTION);
      return listConfigurations(configs, showIndices);
    }

    String[] launchNames = commandLine.getUnrecognizedArgs();

    //Only one name is allowed
    if (launchNames.length == 1) {
      return launchConfiguration(launchNames[0], configs, mode);
    } else {
      return ERROR_INVALID_ARGS;
    }
  }

  /**
   * Simply returns the array as a list if project is null, 
   * otherwise it returns of ILaunchConfigurations that are related to the project.
   *
   * @param IProject project to filter with
   * @param configs the list of configurations to filter
   * @return a list of filtered configurations
   */
  private List<ILaunchConfiguration> filterByProject(IProject project, ILaunchConfiguration[] configs) throws Exception {
    ArrayList<ILaunchConfiguration> list = new ArrayList<ILaunchConfiguration>(configs.length);
    for (ILaunchConfiguration config: configs) {
      list.add(config);
    }
    return list;
  }

  /**
   * lists the configurations for selected project. Returns for the selected workspace if project is null
   * @param project project to list configurations for.
   * @param showIndices whether or not to show the indices for the configurations
   * @return formatted string of configurations
   */
  private String listConfigurations(List<ILaunchConfiguration> configs, boolean showIndices) {
    StringBuilder builder = new StringBuilder();
    int index = 0;
    for (ILaunchConfiguration config:configs) {
      //shows indices of each launcher in case user wants to launch by index
      if (showIndices) {
        builder.append(String.format(FORMAT_SHOW_INDICES,index++,config.getName()));
      } else {
        builder.append(String.format(FORMAT_NO_INDICES,config.getName()));
      }
    }
    return builder.toString();
  }

  /**
   * checks the commandLine to get the mode.
   * @param commandLine commandLine to check
   * @return mode that can be used by DebugUITools to launch configuration
   */
  private String getMode(CommandLine commandLine) {
    String mode;
    if (commandLine.hasOption(Options.DEBUG_OPTION)) {
      mode = IDebugUIConstants.ID_DEBUG_LAUNCH_GROUP;
    } else {
      mode = IDebugUIConstants.ID_RUN_LAUNCH_GROUP;
    }
    return DebugUIPlugin.getDefault().getLaunchConfigurationManager().getLaunchGroup(mode).getMode();
  }

  /**
   * Gets the project from the command line
   * @param commandLine commandLine to get the project from.
   * @return null if the option is not set otherwise the project indicated
   */
  private IProject getProject(CommandLine commandLine) throws Exception {
    if (commandLine.hasOption(Options.PROJECT_OPTION)) {
      String name = commandLine.getValue(Options.PROJECT_OPTION);

      return ProjectUtils.getProject(name, true);
    }
    return null;
  }

  /**
   * Finds and launches the configuration.
   * @param name name of the configuration
   * @param configs list of launch configurations
   * @return response message
   */
  private String launchConfiguration(String name, List<ILaunchConfiguration> configs, String mode) {
    ILaunchConfiguration found = null;
    try {
      //checks if given an index
      int index = Integer.parseInt(name);
      if (index < 0 || index >= configs.size()) {
        return ERROR_NOT_IN_RANGE;
      }
      found = configs.get(index);
    } catch (NumberFormatException e) {
      //Searches for configuration instead of index
      for (ILaunchConfiguration config:configs) {
        if (config.getName().startsWith(name)) {
          //Checks if found has already been set
          if (found != null) {
            return ERROR_MULTIPLE_VAL;
          }
          found = config;
        }
      }
      if (found == null) {
        return ERROR_NOT_FOUND;
      }
    }
    //launches the found LaunchConfiguration
    DebugUITools.launch(found, mode);
    return String.format(FORMAT_STARTED, found.getName());
  }
}
