package com.driver.service;


import com.driver.Group;
import com.driver.Message;
import com.driver.User;
import com.driver.exception.DataNotFoundException;
import com.driver.exception.DuplicateDataException;
import com.driver.repo.WhatsappRepository;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class WhatsappService {
    WhatsappRepository whatsappRepository;

    public WhatsappService() {
        this.whatsappRepository = new WhatsappRepository();
    }

    public String createUser(String name, String mobile) throws DuplicateDataException {
        return whatsappRepository.createUser(name, mobile);
    }

    public Group createGroup(List<User> users) {
        var admin = users.get(0);
        var otherUsers = users.subList(1, users.size());
        if (otherUsers.size() == 1) {
            return whatsappRepository.createPersonalChat(admin, otherUsers.get(0));
        } else {
            return whatsappRepository.createGroupChat(admin, otherUsers);
        }

    }

    public int createMessage(String content) {
        return whatsappRepository.createMessage(content);
    }

    public int sendMessage(Message message, User sender, Group group) throws DataNotFoundException {
        return whatsappRepository.sendMessage(message, sender, group);
    }

    public String changeAdmin(User approver, User user, Group group) throws DataNotFoundException {
        return whatsappRepository.changeAdmin(approver, user, group);
    }

    public int removeUser(User user) throws DataNotFoundException {
        return whatsappRepository.removeUser(user);
    }

    public String findMessage(Date start, Date end, int k) {
        return whatsappRepository.findMessage(start, end, k);
    }
}
