/*
 * Copyright (C) open knowledge GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 */
package de.openknowledge.sample.customer.application;

import static java.lang.String.format;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterStyle;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameters;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;

import de.openknowledge.sample.customer.domain.Customer;
import de.openknowledge.sample.customer.domain.CustomerNotFoundException;
import de.openknowledge.sample.customer.domain.CustomerRepository;
import de.openknowledge.sample.customer.domain.CustomerStatus;
import de.openknowledge.sample.customer.domain.Name;

/**
 * A resource that provides access to the {@link Customer} entity.
 */
@Path("customers")
@ApplicationScoped
public class CustomerResource {

    private static final Logger LOG = Logger.getLogger(CustomerResource.class.getName());

    @Inject
    private CustomerRepository repository;

    private Map<Long, List<AsyncResponse>> statusUpdateListeners = new ConcurrentHashMap<>();

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @RequestBody(name = "Customer", content = {
            @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = CustomerResourceType.class)),
    })
    public Response createCustomer(CustomerResourceType customer, @Context UriInfo uriInfo) {
        LOG.log(Level.INFO, "Create customer {0}", customer);

        Customer newCustomer = new Customer();
        newCustomer.setName(new Name(customer.getFirstName(), customer.getLastName()));
        newCustomer.setEmailAddress(customer.getEmailAddress());
        newCustomer.setGender(customer.getGender());

        Customer createdCustomer = repository.create(newCustomer);
        URI location = uriInfo.getAbsolutePathBuilder().path(createdCustomer.getId().toString()).build();

        LOG.log(Level.INFO, "Customer created at {0}", location);

        return Response.status(Status.CREATED).location(location).build();
    }

    @DELETE
    @Path("/{id}")
    public Response deleteCustomer(@PathParam("id") Long customerId) {
        LOG.log(Level.INFO, "Delete customer with id {0}", customerId);

        try {
            Customer customer = repository.find(customerId);
            repository.delete(customer);

            LOG.info("Customer deleted");

            return Response.status(Status.NO_CONTENT).build();
        } catch (CustomerNotFoundException e) {
            LOG.log(Level.WARNING, "Customer with id {0} not found", customerId);
            return Response.status(Status.NOT_FOUND).build();
        }
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCustomer(@PathParam("id") Long customerId) {
        LOG.log(Level.INFO, "Find customer with id {0}", customerId);

        try {
            Customer customer = repository.find(customerId);
            CustomerResourceType customerResourceType = new CustomerResourceType(customer);

            LOG.log(Level.INFO, "Found customer {0}", customerResourceType);

            return Response.ok(customerResourceType).build();
        } catch (CustomerNotFoundException e) {
            LOG.log(Level.WARNING, "Customer with id {0} not found", customerId);
            throw new NotFoundException("Customer not found");
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<CustomerResourceType> getCustomers() {
        LOG.info("Find all customers");

        List<CustomerResourceType> customers = repository.findAll().stream().map(CustomerResourceType::new)
                .collect(Collectors.toList());

        LOG.log(Level.INFO, "Found {0} customers", customers.size());

        return customers;
    }
    @GET
    @Path("/{id}/status")
    @Parameters({
        @Parameter(
            name = "If-None-Match",
            description = "the previous status of the customer",
            in = ParameterIn.HEADER,
            schema = @Schema(type = SchemaType.STRING),
            style = ParameterStyle.SIMPLE)
    })
    @APIResponses({
        @APIResponse(
            responseCode = "304",
            description = "status has not changed since last request and no Prefer header set"),
        @APIResponse(
            responseCode = "412",
            description = "status has not changed after wait time")
    })
    public void getStatus(
            @PathParam("id") Long customerId,
            @Parameter(
                name = "Prefer",
                description = "initiate long-polling request",
                in = ParameterIn.HEADER,
                example = "wait=100",
                schema = @Schema(type = SchemaType.STRING),
                style = ParameterStyle.SIMPLE
            )
            @HeaderParam("Prefer") String preferHeader,
            @Context Request request,
            @Suspended AsyncResponse response) {
        CustomerStatus status = repository.findStatus(customerId);
        EntityTag tag = new EntityTag(status.name());
        
        Optional<ResponseBuilder> conditionalResponse = Optional.ofNullable(request.evaluatePreconditions(tag));
        if (!conditionalResponse.isPresent()) {
            LOG.info(() -> format("Preconditions don't match, directly returning status %s for Customer#%s", status, customerId));
            response.resume(Response.ok(status).cacheControl(getCachingConfiguration()).tag(tag).build());
        } else if (preferHeader != null) {
            LOG.info("Preconditions match, check for long-polling requirement");
            Stream<String> preferHeaderValues = Stream.of(preferHeader.split(";")).map(s -> s.toLowerCase().trim());
            Optional<String> waitHeader = preferHeaderValues.filter(s -> s.startsWith("wait=")).map(s -> s.substring("wait=".length())).findAny();
            if (waitHeader.isPresent()) {
                LOG.info("wait header present, start long-polling");
                response.setTimeout(Integer.parseInt(waitHeader.get()), TimeUnit.SECONDS);
                response.setTimeoutHandler(r -> r.resume(Response.status(Status.PRECONDITION_FAILED).build()));
                List<AsyncResponse> responses = statusUpdateListeners.computeIfAbsent(customerId, id -> new ArrayList<>());
                responses.add(response);
            } else {
                LOG.info("no wait header present, directly returning 304");
                response.resume(conditionalResponse.get().build());
            }
        } else {
            LOG.info("no wait header present, directly returning 304");
            response.resume(conditionalResponse.get().build());
        }
    }

    @PUT
    @Path("/{id}/status")
    public void updateStatus(@PathParam("id") Long customerId, CustomerStatus status) {
        repository.updateStatus(customerId, status);
        Optional<List<AsyncResponse>> updateListeners = Optional.ofNullable(statusUpdateListeners.remove(customerId));
        LOG.info(format("Propagating status update to %d listeners", updateListeners.map(Collection::size).orElse(0)));
        updateListeners.ifPresent(l -> l.stream().filter(r -> !r.isCancelled()).forEach(r -> r.resume(status)));
    }

    private CacheControl getCachingConfiguration() {
        CacheControl cacheControl = new CacheControl();
        cacheControl.setMaxAge(1000);
        cacheControl.setPrivate(true);
        cacheControl.setNoStore(true);
        return cacheControl;
    }
}
