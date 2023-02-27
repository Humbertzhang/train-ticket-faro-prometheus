package contacts.service;

import contacts.entity.*;
import edu.fudan.common.util.Response;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import contacts.repository.ContactsRepository;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.UUID;


/**
 * @author fdse
 */
@Service
public class ContactsServiceImpl implements ContactsService {

    private Counter get_contacts_ErrorCounter;
    private Counter post_contacts_ErrorCounter;
    private Counter post_contacts_admin_ErrorCounter;
    private Counter delete_contacts_contactsId_ErrorCounter;
    private Counter put_contacts_ErrorCounter;
    private Counter get_contacts_account_accountId_ErrorCounter;
    private Counter get_contacts_id_ErrorCounter;


    @Autowired
    private MeterRegistry meterRegistry;

    @PostConstruct
    public void init() {
        Tags tags = Tags.of("service", "ts-contacts-service");
        meterRegistry.config().commonTags(tags);
        get_contacts_ErrorCounter = Counter.builder("request.get.contacts.error").register(meterRegistry);
        post_contacts_ErrorCounter = Counter.builder("request.post.contacts.error").register(meterRegistry);
        post_contacts_admin_ErrorCounter = Counter.builder("request.post.contacts.admin.error").register(meterRegistry);
        delete_contacts_contactsId_ErrorCounter = Counter.builder("request.delete.contacts.contactsId.error").register(meterRegistry);
        put_contacts_ErrorCounter = Counter.builder("request.put.contacts.error").register(meterRegistry);
        get_contacts_account_accountId_ErrorCounter = Counter.builder("request.get.contacts.account.accountId.error").register(meterRegistry);
        get_contacts_id_ErrorCounter = Counter.builder("request.get.contacts.id.error").register(meterRegistry);
    }

    @Autowired
    private ContactsRepository contactsRepository;

    String success = "Success";

    private static final Logger LOGGER = LoggerFactory.getLogger(ContactsServiceImpl.class);

    @Override
    public Response findContactsById(String id, HttpHeaders headers) {
        LOGGER.info("FIND CONTACTS BY ID: " + id);
        Contacts contacts = contactsRepository.findById(id).orElse(null);
        if (contacts != null) {
            return new Response<>(1, success, contacts);
        } else {
            get_contacts_id_ErrorCounter.increment();
            LOGGER.error("[findContactsById][contactsRepository.findById][No contacts according to contactsId][contactsId: {}]", id);
            return new Response<>(0, "No contacts according to contacts id", null);
        }
    }

    @Override
    public Response findContactsByAccountId(String accountId, HttpHeaders headers) {
        ArrayList<Contacts> arr = contactsRepository.findByAccountId(accountId);
        ContactsServiceImpl.LOGGER.info("[findContactsByAccountId][Query Contacts][Result Size: {}]", arr.size());
        return new Response<>(1, success, arr);
    }

    @Override
    public Response createContacts(Contacts contacts, HttpHeaders headers) {
        Contacts contactsTemp = contactsRepository.findByAccountIdAndDocumentTypeAndDocumentType(contacts.getAccountId(), contacts.getDocumentNumber(), contacts.getDocumentType());
        if (contactsTemp != null) {
            post_contacts_admin_ErrorCounter.increment();
            ContactsServiceImpl.LOGGER.warn("[createContacts][Init Contacts, Already Exists][Id: {}]", contacts.getId());
            return new Response<>(0, "Already Exists", contactsTemp);
        } else {
            contactsRepository.save(contacts);
            return new Response<>(1, "Create Success", null);
        }
    }

    @Override
    public Response create(Contacts addContacts, HttpHeaders headers) {

        Contacts c = contactsRepository.findByAccountIdAndDocumentTypeAndDocumentType(addContacts.getAccountId(), addContacts.getDocumentNumber(), addContacts.getDocumentType());

        if (c != null) {
            post_contacts_ErrorCounter.increment();
            ContactsServiceImpl.LOGGER.warn("[Contacts-Add&Delete-Service.create][AddContacts][Fail.Contacts already exists][contactId: {}]", addContacts.getId());
            return new Response<>(0, "Contacts already exists", null);
        } else {
            Contacts contacts = contactsRepository.save(addContacts);
            ContactsServiceImpl.LOGGER.info("[Contacts-Add&Delete-Service.create][AddContacts Success]");
            return new Response<>(1, "Create contacts success", contacts);
        }
    }

    @Override
    public Response delete(String contactsId, HttpHeaders headers) {
        contactsRepository.deleteById(contactsId);
        Contacts contacts = contactsRepository.findById(contactsId).orElse(null);
        if (contacts == null) {
            ContactsServiceImpl.LOGGER.info("[Contacts-Add&Delete-Service][DeleteContacts Success]");
            return new Response<>(1, "Delete success", contactsId);
        } else {
            delete_contacts_contactsId_ErrorCounter.increment();
            ContactsServiceImpl.LOGGER.error("[Contacts-Add&Delete-Service][DeleteContacts][Fail.Reason not clear][contactsId: {}]", contactsId);
            return new Response<>(0, "Delete failed", contactsId);
        }
    }

    @Override
    public Response modify(Contacts contacts, HttpHeaders headers) {
        headers = null;
        Response oldContactResponse = findContactsById(contacts.getId(), headers);
        LOGGER.info(oldContactResponse.toString());
        Contacts oldContacts = (Contacts) oldContactResponse.getData();
        if (oldContacts == null) {
            put_contacts_ErrorCounter.increment();
            ContactsServiceImpl.LOGGER.error("[Contacts-Modify-Service.modify][ModifyContacts][Fail.Contacts not found][contactId: {}]", contacts.getId());
            return new Response<>(0, "Contacts not found", null);
        } else {
            oldContacts.setName(contacts.getName());
            oldContacts.setDocumentType(contacts.getDocumentType());
            oldContacts.setDocumentNumber(contacts.getDocumentNumber());
            oldContacts.setPhoneNumber(contacts.getPhoneNumber());
            contactsRepository.save(oldContacts);
            ContactsServiceImpl.LOGGER.info("[Contacts-Modify-Service.modify][ModifyContacts Success]");
            return new Response<>(1, "Modify success", oldContacts);
        }
    }

    @Override
    public Response getAllContacts(HttpHeaders headers) {
        ArrayList<Contacts> contacts = contactsRepository.findAll();
        if (contacts != null && !contacts.isEmpty()) {
            return new Response<>(1, success, contacts);
        } else {
            get_contacts_ErrorCounter.increment();
            LOGGER.error("[getAllContacts][contactsRepository.findAll][Get all contacts error][message: {}]", "No content");
            return new Response<>(0, "No content", null);
        }
    }

}


