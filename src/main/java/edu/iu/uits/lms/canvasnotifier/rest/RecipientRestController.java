package edu.iu.uits.lms.canvasnotifier.rest;

import edu.iu.uits.lms.canvasnotifier.model.Recipient;
import edu.iu.uits.lms.canvasnotifier.repository.RecipientRepository;
//import edu.iu.uits.lms.swagger.LmsSwaggerDocumentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping({"/rest/recipient"})
//@LmsSwaggerDocumentation
public class RecipientRestController {

    @Autowired
    private RecipientRepository recipientRepository;

    @RequestMapping(value = "/{id}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public Recipient getRecipientFromId(@PathVariable Long id) {
        Recipient recipient = recipientRepository.findById(id).orElse(null);
        return recipient;
    }

    @RequestMapping(value = "/job/{id}", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    public List<Recipient> getRecipientsForJob(@PathVariable Long id) {
        List<Recipient> recipients = (List<Recipient>) recipientRepository.findByJob(id);

        if (recipients != null) {
            recipients  = recipients.stream()
                    .sorted(Comparator.comparing(Recipient::getUsername, Comparator.nullsLast(Comparator.naturalOrder()))).collect(Collectors.toList());
        }

        return recipients;
    }
}
