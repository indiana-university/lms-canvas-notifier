package edu.iu.uits.lms.canvasnotifier.model;

/*-
 * #%L
 * canvasnotifier
 * %%
 * Copyright (C) 2015 - 2022 Indiana University
 * %%
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the Indiana University nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;

@Entity
@Table(name = "CANVASNOTIFIER_USERS")
@NamedQueries({
        @NamedQuery(name = "User.findByUsername", query = "from User where username = :username"),
        @NamedQuery(name = "User.findByCanvasUserId", query = "from User where canvasUserId = :canvasUserId"),
        @NamedQuery(name = "User.findAllAuthorizedSenders", query = "from User where authorizedSender = true order by displayName asc"),
        @NamedQuery(name = "User.findAllAuthorizedUsers", query = "from User where authorizedUser = true order by displayName asc")
})

@SequenceGenerator(name = "CANVASNOTIFIER_USERS_ID_SEQ", sequenceName = "CANVASNOTIFIER_USERS_ID_SEQ", allocationSize = 1)
@Data
@RequiredArgsConstructor
public class User extends ModelWithDates implements Serializable {

   @Id
   @GeneratedValue(generator = "CANVASNOTIFIER_USERS_ID_SEQ")
   @Column(name = "CANVASNOTIFIER_USERS_ID")
   private Long id;

   @Column(name = "DISPLAY_NAME")
   private String displayName;

   @Column(name = "USERNAME")
   private String username;

   @Column(name = "CANVAS_USER_ID")
   private String canvasUserId;

   @Column(name = "IS_AUTHORIZED_SENDER")
   private boolean authorizedSender;

   @Column(name = "IS_AUTHORIZED_USER")
   private boolean authorizedUser;
}
