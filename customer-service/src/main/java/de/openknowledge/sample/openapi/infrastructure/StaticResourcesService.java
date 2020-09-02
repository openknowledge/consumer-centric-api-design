/*
 * Copyright 2019 open knowledge GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.openknowledge.sample.openapi.infrastructure;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

@ApplicationScoped
@Path("{path: webjars/.*}")
public class StaticResourcesService {

	private final static Logger LOG = Logger.getLogger(StaticResourcesService.class.getSimpleName());

    @GET
    public Response staticJsResources(@PathParam("path") final String path) {
        try (InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(String.format("META-INF/resources/%s", path));
                BufferedReader buffer = new BufferedReader(new InputStreamReader(inputStream));
                StringWriter stringWriter = new StringWriter()) {
            buffer.lines().forEach(stringWriter::write);

            String response = stringWriter.toString();
            if (response != null && !response.isEmpty()) {
                Response.ResponseBuilder ret = Response.ok(response);
                if (path.endsWith(".js")) {
                    ret = ret.type("application/javascript");
                }
                return ret.build();
            }
        } catch (IOException ex) {
        	LOG.severe(ex.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        LOG.log(Level.WARNING, "Could not find resource [{0}]", path);
        return Response.status(Response.Status.NOT_FOUND).build();
    }
    
}