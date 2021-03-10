package edu.iu.uits.lms.canvasnotifier.amqp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CanvasNotifierMessage implements Serializable {
   private Long id;
}
