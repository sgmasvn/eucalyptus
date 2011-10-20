/*******************************************************************************
 * Copyright (c) 2009  Eucalyptus Systems, Inc.
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, only version 3 of the License.
 * 
 * 
 *  This file is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  for more details.
 * 
 *  You should have received a copy of the GNU General Public License along
 *  with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 *  Please contact Eucalyptus Systems, Inc., 130 Castilian
 *  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
 *  if you need additional information or have any questions.
 * 
 *  This file may incorporate work covered under the following copyright and
 *  permission notice:
 * 
 *    Software License Agreement (BSD License)
 * 
 *    Copyright (c) 2008, Regents of the University of California
 *    All rights reserved.
 * 
 *    Redistribution and use of this software in source and binary forms, with
 *    or without modification, are permitted provided that the following
 *    conditions are met:
 * 
 *      Redistributions of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 * 
 *      Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 * 
 *    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 *    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 *    TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 *    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 *    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 *    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
 *    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
 *    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
 *    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
 *    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
 *    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
 *    THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
 *    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
 *    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
 *    ANY SUCH LICENSES OR RIGHTS.
 *******************************************************************************
 * @author chris grzegorczyk <grze@eucalyptus.com>
 */

package com.eucalyptus.component;

import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.apache.log4j.Logger;
import com.eucalyptus.bootstrap.Handles;
import com.eucalyptus.bootstrap.ServiceJarDiscovery;
import com.eucalyptus.system.Ats;
import com.eucalyptus.util.TypeMapper;
import com.google.common.base.Function;
import com.google.common.collect.Maps;

public class ServiceBuilders {
  private static Logger                                                           LOG               = Logger.getLogger( ServiceBuilders.class );
  private static Map<Class, ServiceBuilder<? extends ServiceConfiguration>>       builders          = Maps.newConcurrentMap( );
  private static Map<ComponentId, ServiceBuilder<? extends ServiceConfiguration>> componentBuilders = Maps.newConcurrentMap( );
  
  public static class ServiceBuilderDiscovery extends ServiceJarDiscovery {
    
    @Override
    public Double getPriority( ) {
      return 0.2;
    }
    
    @Override
    public boolean processClass( Class candidate ) throws Exception {
      if ( ServiceBuilder.class.isAssignableFrom( candidate ) && !Modifier.isAbstract( candidate.getModifiers( ) )
           && !Modifier.isInterface( candidate.getModifiers( ) ) ) {
        /** GRZE: this implies that service builder is a singleton **/
        ServiceBuilder b = ( ServiceBuilder ) candidate.newInstance( );
        if ( Ats.from( candidate ).has( DiscoverableServiceBuilder.class ) ) {
          DiscoverableServiceBuilder at = Ats.from( candidate ).get( DiscoverableServiceBuilder.class );
          for ( Class c : at.value( ) ) {
            ComponentId compId = ( ComponentId ) c.newInstance( );
            ServiceBuilders.addBuilder( compId, b );
          }
        }
        if ( Ats.from( candidate ).has( Handles.class ) ) {
          for ( Class c : Ats.from( candidate ).get( Handles.class ).value( ) ) {
            ServiceBuilders.addBuilder( c, b );
          }
        }
        return true;
      } else {
        return false;
      }
    }
    
  }
  
  static void addBuilder( Class c, ServiceBuilder b ) {
    LOG.trace( "Registered service builder for " + c.getSimpleName( ) + " -> " + b.getClass( ).getCanonicalName( ) );
    builders.put( c, b );
  }
  
  public static void addBuilder( ComponentId c, ServiceBuilder b ) {
    LOG.trace( "Registered service builder for " + c.name( ) + " -> " + b.getClass( ).getCanonicalName( ) );
    componentBuilders.put( c, b );
  }
  
  public static Set<Entry<Class, ServiceBuilder<? extends ServiceConfiguration>>> entrySet( ) {
    return builders.entrySet( );
  }
  
  public static ServiceBuilder<? extends ServiceConfiguration> handles( Class handlesType ) {
    return builders.get( handlesType );
  }
  
  public static ServiceBuilder<? extends ServiceConfiguration> lookup( ComponentId componentId ) {
    if ( !componentBuilders.containsKey( componentId ) ) {
      Component comp = Components.lookup( componentId );
      componentBuilders.put( componentId, new DummyServiceBuilder( comp.getComponentId( ) ) );
    }
    return componentBuilders.get( componentId );
  }
  
  public static ServiceBuilder<? extends ServiceConfiguration> lookup( Class<? extends ComponentId> componentIdClass ) {
    try {
      return lookup( componentIdClass.newInstance( ) );
    } catch ( Exception ex ) {
      LOG.error( ex, ex );
      throw new RuntimeException( ex );
    }
  }
  
  @TypeMapper
  public enum ServiceBuilderMapper implements Function<ServiceConfiguration, ServiceBuilder<? extends ServiceConfiguration>> {
    INSTANCE;
    
    @Override
    public ServiceBuilder<? extends ServiceConfiguration> apply( final ServiceConfiguration input ) {
      return ServiceBuilders.lookup( input.getComponentId( ) );
    }
    
  }

  public static ComponentId oneWhichHandles( final Class c ) {
    return handles( c ).getComponentId( );
  }
}
