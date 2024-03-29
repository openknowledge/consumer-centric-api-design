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

import static org.eclipse.microprofile.openapi.annotations.enums.ParameterIn.HEADER;

import java.net.URI;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.eclipse.microprofile.openapi.annotations.headers.Header;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

import de.openknowledge.sample.customer.domain.Customer;
import de.openknowledge.sample.customer.domain.CustomerNotFoundException;
import de.openknowledge.sample.customer.domain.CustomerRepository;
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

            return Response.ok(customerResourceType).tag(compute(customer)).build();
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

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Parameter(name = "If-Match", in = HEADER, description = "The ETag of the customer to update")
    @RequestBody(name = "Customer", content = {
            @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = CustomerResourceType.class))
    })
    @APIResponse(responseCode = "204", description = "The customer was updated successfully", headers = @Header(name = "ETag", description = "The ETag of the updated customer"))
    @APIResponse(responseCode = "412", description = "The provided ETag does not match the current ETag of the customer")
    @APIResponse(responseCode = "428", description = "ETag required to update the customer, but none provided")
    public Response updateCustomer(@PathParam("id") Long customerId, @HeaderParam("If-Match") String ifMatch, @Context Request request, CustomerResourceType modifiedCustomer) {
        LOG.log(Level.INFO, "Update customer with id {0}", customerId);

        if (ifMatch == null) {
        	return Response.status(Status.PRECONDITION_REQUIRED).build();
        }
        try {
            Customer foundCustomer = repository.find(customerId);
            EntityTag current = compute(foundCustomer);
            ResponseBuilder preconditionFailed = request.evaluatePreconditions(current);
            if (preconditionFailed != null) {
            	return preconditionFailed.build();
            }

            foundCustomer.setName(new Name(modifiedCustomer.getFirstName(), modifiedCustomer.getLastName()));
            foundCustomer.setEmailAddress(modifiedCustomer.getEmailAddress());
            foundCustomer.setGender(modifiedCustomer.getGender());

            Customer updatedCustomer = repository.update(foundCustomer);

            LOG.log(Level.INFO, "Customer updated {0}", updatedCustomer);

            return Response.status(Status.NO_CONTENT).build();
        } catch (CustomerNotFoundException e) {
            LOG.log(Level.WARNING, "Customer with id {0} not found", customerId);
            return Response.status(Status.NOT_FOUND).build();
        }
    }

    private EntityTag compute(Customer customer) {
    	return new EntityTag(Integer.toString(customer.getName().toString().hashCode() ^ customer.getEmailAddress().hashCode() ^ customer.getGender().hashCode()));
    }
}
