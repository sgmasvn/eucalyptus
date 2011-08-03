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

package com.eucalyptus.cloud;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Transient;
import com.eucalyptus.auth.principal.AccountFullName;
import com.eucalyptus.auth.principal.FakePrincipals;
import com.eucalyptus.auth.principal.UserFullName;
import com.eucalyptus.util.HasOwningUser;
import com.eucalyptus.util.OwnerFullName;

@MappedSuperclass
public abstract class UserMetadata<STATE extends Enum<STATE>> extends AccountMetadata<STATE> implements HasOwningUser {
  @Column( name = "metadata_user_id" )
  protected String      ownerUserId;
  @Column( name = "metadata_user_name" )
  protected String      ownerUserName;
  @Column( name = "metadata_perm_uuid", unique = true, updatable = false, nullable = false )
  private String        permanentUuid;
  @Transient
  private OwnerFullName tempOwnerFullName;
  
  /**
   * GRZE:NOTE: Should only /ever/ be used by sub classes.
   */
  protected UserMetadata( ) {}
  
  /**
   * GRZE:NOTE: Should only /ever/ be used by sub classes.
   */
  protected UserMetadata( OwnerFullName owner ) {
    super( owner );
    this.setOwner( owner );
  }
  
  /**
   * GRZE:NOTE: Should only /ever/ be used by sub classes.
   */
  protected UserMetadata( OwnerFullName owner, String displayName ) {
    super( owner, displayName );
    this.setOwner( owner );
  }
  
  @Override
  public void setOwner( OwnerFullName owner ) {
    this.ownerUserId = owner != null
      ? owner.getUniqueId( )
      : null;
    this.ownerUserName = owner != null
      ? owner.getUserName( )
      : null;
    super.setOwner( owner );
    this.tempOwnerFullName = owner;
  }
  
  @Override
  public OwnerFullName getOwner( ) {
    if ( this.tempOwnerFullName == null ) {
      OwnerFullName tempOwner = super.getOwner( );
      if ( super.getOwnerAccountNumber( ) == null && this.getOwnerUserId( ) == null ) {
        tempOwner = FakePrincipals.NOBODY_USER_ERN;
      } else if ( this.getOwnerAccountNumber( ) == null && FakePrincipals.NOBODY_USER_ERN.getUserId( ).equals( this.getOwnerUserId( ) ) ) {
        tempOwner = FakePrincipals.NOBODY_USER_ERN;
      } else if ( this.getOwnerAccountNumber( ) != null && this.getOwnerUserId( ) == null ) {
        tempOwner = AccountFullName.getInstance( this.getOwnerAccountNumber( ) );
      } else if ( this.getOwnerAccountNumber( ) == null && this.getOwnerUserId( ) != null ) {
        tempOwner = UserFullName.getInstance( this.getOwnerUserId( ) );
      }
      this.tempOwnerFullName = tempOwner;
      return this.tempOwnerFullName;
    } else {
      return super.getOwner( );
    }
  }
  
  @Override
  public int hashCode( ) {
    final int prime = 31;
    int result = super.hashCode( );
    result = prime * result + ( ( ownerUserId == null )
      ? 0
      : ownerUserId.hashCode( ) );
    return result;
  }
  
  @Override
  public String getOwnerUserId( ) {
    return this.ownerUserId;
  }
  
  public void setOwnerUserId( String ownerUserId ) {
    this.ownerUserId = ownerUserId;
  }
  
  @Override
  public String getOwnerUserName( ) {
    return this.ownerUserName;
  }
  
  public void setOwnerUserName( String ownerUserName ) {
    this.ownerUserName = ownerUserName;
  }
  
  @PrePersist
  @PreUpdate
  public void verifyComplete( ) {
    assertThat( this.ownerUserId, notNullValue( ) );
    assertThat( this.ownerUserName, notNullValue( ) );
    if ( this.permanentUuid == null ) {
      this.permanentUuid = UUID.randomUUID( ).toString( );
    }
  }
  
  public String getPermanentUuid( ) {
    return this.permanentUuid;
  }
  
}
