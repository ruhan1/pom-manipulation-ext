/*
 * Copyright (C) 2012 Red Hat, Inc. (jcasey@redhat.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.maven.ext.manip.rest;

import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.ref.SimpleProjectVersionRef;
import org.commonjava.maven.ext.manip.rest.exception.RestException;
import org.commonjava.maven.ext.manip.rest.rule.MockServer;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.SystemOutRule;
import org.junit.rules.TestName;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import static org.commonjava.maven.ext.manip.rest.DefaultVersionTranslator.RestProtocol;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class HttpHeaderHeaderTest
{
    private DefaultVersionTranslator versionTranslator;

    @Rule
    public final SystemOutRule systemOutRule = new SystemOutRule().enableLog();

    @Rule
    public final TestName testName = new TestName();

    @Rule
    public final MockServer mockServer = new MockServer( new AbstractHandler()
    {
        @Override
        public void handle( String target, Request baseRequest, HttpServletRequest request,
                            HttpServletResponse response )
                        throws IOException, ServletException
        {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);

            Enumeration<String> names = request.getHeaderNames();
            while ( names.hasMoreElements() )
            {
                String name = names.nextElement();

                if ( name.equals( "Log-Context" ) )
                {
                    response.getWriter().print( HttpHeaderHeaderTest.this.generateResponse( request.getHeader( name ) ) );
                }
            }
            baseRequest.setHandled( true );
        }
    } );

    @Before
    public void before()
    {
        LoggerFactory.getLogger( DefaultVersionTranslator.class ).info ("Executing test " + testName.getMethodName());

        this.versionTranslator = new DefaultVersionTranslator( mockServer.getUrl(), RestProtocol.DEPRECATED );
    }

    private String generateResponse( String header )
    {
        return testResponseStart + (testResponseEnd == null ? "" : header + testResponseEnd);
    }

    private String testResponseStart = "{\\\"message\\\":\\\"";
    private String testResponseEnd = "\\\"}";

    @Test
    public void testVerifyContentHeaderMessage()
    {
        List<ProjectVersionRef> gavs = new ArrayList<ProjectVersionRef>()
        {{
            add( new SimpleProjectVersionRef( "com.example", "example", "1.0" ) );
        }};

        try
        {
            versionTranslator.translateVersions( gavs );
            fail( "Failed to throw RestException." );
        }
        catch ( RestException ex )
        {
            assertTrue( systemOutRule.getLog().contains( "message" ) );
        }
    }

    @Test
    public void testVerifyContentHeaderMessageNoEscape()
    {
        testResponseStart = "{\"message\":\"";
        testResponseEnd = "\"}";

        List<ProjectVersionRef> gavs = new ArrayList<ProjectVersionRef>()
        {{
            add( new SimpleProjectVersionRef( "com.example", "example", "1.0" ) );
        }};

        try
        {
            versionTranslator.translateVersions( gavs );
            fail( "Failed to throw RestException." );
        }
        catch ( RestException ex )
        {
            assertTrue( systemOutRule.getLog().contains( "message" ) );
        }
    }

    @Test
    public void testVerifyContentHeaderMessageContents()
    {
        testResponseStart = "{\"message\":\"";
        testResponseEnd = "\"}";

        List<ProjectVersionRef> gavs = new ArrayList<ProjectVersionRef>()
        {{
            add( new SimpleProjectVersionRef( "com.example", "example", "1.0" ) );
        }};

        try
        {
            versionTranslator.translateVersions( gavs );
            fail( "Failed to throw RestException." );
        }
        catch ( RestException ex )
        {
            assertTrue( ex.getMessage().contains( "pme-" ) );
        }
    }

    @Test
    public void testVerifyContentHeaderMessageContentsHTML()
    {
        testResponseStart = "<html><body><h1>504 Gateway Time-out</h1>\n" +
            "The server didn't respond in time.\n" +
            "</body></html>";
        testResponseEnd = null;

        List<ProjectVersionRef> gavs = new ArrayList<ProjectVersionRef>()
        {{
            add( new SimpleProjectVersionRef( "com.example", "example", "1.0" ) );
        }};

        try
        {
            versionTranslator.translateVersions( gavs );
            fail( "Failed to throw RestException." );
        }
        catch ( RestException ex )
        {
            assertTrue( systemOutRule.getLog().contains( "The server didn't respond in time" ) );
        }
    }
}
