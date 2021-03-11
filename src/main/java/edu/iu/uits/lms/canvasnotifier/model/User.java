package edu.iu.uits.lms.canvasnotifier.model;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.io.Serializable;

@Entity
@Table(name = "CANVASNOTIFIER_USERS")
@NamedQueries({
        @NamedQuery(name = "User.findByUsername", query = "from User where username = :username"),
        @NamedQuery(name = "User.findByCanvasUserId", query = "from User where canvas_user_id = :canvasUserId"),
        @NamedQuery(name = "User.findAllAuthorizedSenders", query = "from User where authorizedSender = true order by username asc"),
        @NamedQuery(name = "User.findAllAuthorizedUsers", query = "from User where authorizedUser = true order by username asc")
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
