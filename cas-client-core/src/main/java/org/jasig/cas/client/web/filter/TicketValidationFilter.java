/*
 * Copyright 2006 The JA-SIG Collaborative. All rights reserved. See license
 * distributed with this file and available online at
 * http://www.ja-sig.org/products/cas/overview/license/index.html
 */
package org.jasig.cas.client.web.filter;

import org.jasig.cas.authentication.principal.SimpleService;
import org.jasig.cas.client.util.CommonUtils;
import org.jasig.cas.client.validation.Assertion;
import org.jasig.cas.client.validation.TicketValidator;
import org.jasig.cas.client.validation.ValidationException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Implementation of a Filter that checks for a "ticket" and if one is found,
 * will attempt to validate the ticket. On a successful validation, it sets the
 * Assertion object into the session. On an unsuccessful validation attempt, it
 * sets the response code to 403.
 * <p/>
 * This filter needs to be configured after the authentication filter (if that
 * filter exists in the chain).
 *
 * @author Scott Battaglia
 * @version $Revision$ $Date$
 * @see TicketValidator
 * @since 3.0
 */
public final class TicketValidationFilter extends AbstractCasFilter {

    /**
     * Instance of the ticket validator.
     */
    private final TicketValidator ticketValidator;

    /**
     * Specify whether the filter should redirect the user agent after a
     * successful validation to remove the ticket parameter from the query
     * string.
     */
    private final boolean redirectAfterValidation;

    /** Determines whether an exception is thrown when there is a ticket validation failure. */
    private final boolean exceptionOnValidationFailure;

    /**
     * Constructor that takes the severName (or serviceUrl) and the TicketValidator.  Either serveName or serviceUrl is required (but not both).
     *
     * @param service              the name of the server in <hostname>:<port> combination, if using a non-standard port or the fully qualified url.
     * @param isServerName              whether the service is the host name or the fully qualified url.
     * @param ticketValidator the validator to validate the tickets.
     */
    public TicketValidationFilter(final String service, final boolean isServerName, final TicketValidator ticketValidator) {
        this(service, isServerName, true, ticketValidator, false);
    }

    /**
     * Constructor that takes the severName (or serviceUrl), TicketValidator, useSession and redirectAfterValidation.  Either serveName or serviceUrl is required (but not both).
     *
     * @param service              the name of the server in <hostname>:<port> combination, if using a non-standard port or the fully qualified url.
     * @param isServerName              whether the service is the host name or the fully qualified url.
     * @param useSession              flag to set whether to store stuff in the session.
     * @param ticketValidator         the validator to validate the tickets.
     * @param redirectAfterValidation whether to redirect to remove the ticket.
     */
    public TicketValidationFilter(final String service, final boolean isServerName, final boolean useSession, final TicketValidator ticketValidator, final boolean redirectAfterValidation) {
        this(service, isServerName, useSession, ticketValidator, redirectAfterValidation, true);
    }

    /**
     * Constructor that takes the severName (or serviceUrl), TicketValidator, useSession and redirectAfterValidation.  Either serveName or serviceUrl is required (but not both).
     *
     * @param service              the name of the server in <hostname>:<port> combination, if using a non-standard port or the fully qualified url.
     * @param isServerName              whether the service is the host name or the fully qualified url.
     * @param useSession              flag to set whether to store stuff in the session.
     * @param ticketValidator         the validator to validate the tickets.
     * @param redirectAfterValidation whether to redirect to remove the ticket.
     * @param exceptionOnValidationFailure whether to throw an exception if there is a validation failure or not.
     */
    public TicketValidationFilter(final String service, final boolean isServerName, final boolean useSession, final TicketValidator ticketValidator, final boolean redirectAfterValidation, final boolean exceptionOnValidationFailure) {
        super(service, isServerName, useSession);
        CommonUtils.assertNotNull(ticketValidator,
                "ticketValidator cannot be null.");
        this.ticketValidator = ticketValidator;
        this.redirectAfterValidation = redirectAfterValidation;
        this.exceptionOnValidationFailure = exceptionOnValidationFailure;

        log.info("Initialized with the following properties:  " +
                "ticketValidator=" + this.ticketValidator.getClass().getName() + "; " +
                "redirectAfterValidation=" + this.redirectAfterValidation + "; exceptionOnValidationFailure=" + exceptionOnValidationFailure);

    }

    protected void doFilterInternal(final HttpServletRequest request,
                                    final HttpServletResponse response, final FilterChain filterChain)
            throws IOException, ServletException {
        final String ticket = request.getParameter(PARAM_TICKET);

        if (CommonUtils.isNotBlank(ticket)) {
            if (log.isDebugEnabled()) {
                log.debug("Attempting to validate ticket: " + ticket);
            }

            try {
                final Assertion assertion = this.ticketValidator.validate(
                        ticket, new SimpleService(constructServiceUrl(request,
                        response)));

                if (log.isDebugEnabled()) {
                    log.debug("Successfully authenticated user: "
                            + assertion.getPrincipal().getId());
                }

                request.setAttribute(CONST_PRINCIPAL, assertion.getPrincipal());

                if (isUseSession()) {
                    request.getSession().setAttribute(CONST_ASSERTION,
                            assertion);
                }
            } catch (final ValidationException e) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                log.warn(e, e);

                if (this.exceptionOnValidationFailure) {
                    throw new ServletException(e);
                }
            }

            if (this.redirectAfterValidation) {
                response.sendRedirect(response
                        .encodeRedirectURL(constructServiceUrl(request, response)));
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
