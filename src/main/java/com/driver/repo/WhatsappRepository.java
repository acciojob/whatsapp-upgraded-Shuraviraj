package com.driver.repo;

import com.driver.Group;
import com.driver.Message;
import com.driver.User;
import com.driver.exception.DataNotFoundException;
import com.driver.exception.DuplicateDataException;
import com.driver.validation.WhatsappValidationUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class WhatsappRepository {
    private final Map<String, User> mobileUsers;
    private final Map<Group, List<User>> groupUsers;
    private final List<Message> messages;
    private final List<Group> groups;
    private final List<User> users;
    private final Map<Group, List<Message>> groupMessages;
    private final Map<User, List<Message>> userMessages;


    private int totalNonPersonalGroups = 0;

    public WhatsappRepository() {
        this.messages = new ArrayList<>();
        this.groups = new ArrayList<>();
        this.users = new ArrayList<>();
        this.groupUsers = new HashMap<>();
        this.mobileUsers = new HashMap<>();
        this.groupMessages = new HashMap<>();
        this.userMessages = new HashMap<>();
    }

    public String createUser(String name, String mobile) throws DuplicateDataException {

        // checking if user already exists
        // if user exists throw DuplicateDataException
        if (mobileUsers.containsKey(mobile)) {
            throw new DuplicateDataException("User already exists");
        }
        // else create user and add to users map
        User user = new User(name, mobile);
        mobileUsers.put(mobile, user);
        users.add(user);
        return "SUCCESS";
    }

    public Group createPersonalChat(User admin, User user) {
        //creating group with name and total members as 2
        var group = new Group(user.getName(), 2);

        //adding admin and user to group
        List<User> usersInGroup = new ArrayList<>();

        User adminFromDb = mobileUsers.get(admin.getMobile());
        User userFromDb = mobileUsers.get(user.getMobile());

        usersInGroup.add(adminFromDb);
        usersInGroup.add(userFromDb);

        //updating the personal group ma
        groupUsers.put(group, usersInGroup);

        //adding group to groups list
        groups.add(group);

        return group;
    }

    public Group createGroupChat(User admin, List<User> otherUsers) {
        totalNonPersonalGroups++; // incrementing the total number of non-personal groups

        //creating group with name and total members as number of other users + 1
        var group = new Group("Group " + totalNonPersonalGroups, otherUsers.size() + 1);

        //adding admin to group
        List<User> usersInGroup = new ArrayList<>();

        User adminFromDb = mobileUsers.get(admin.getMobile());
        usersInGroup.add(adminFromDb);

        //adding other users to group
        for(User user : otherUsers) {
            User userFromDb = mobileUsers.get(user.getMobile());
            usersInGroup.add(userFromDb);
        }

        //adding group to groups list
        groups.add(group);

        //updating the non-personal group map
        groupUsers.put(group, usersInGroup);
        return group;
    }

    public int createMessage(String content) {
        //creating message and adding to messages list
        var message = new Message(messages.size() + 1, content, Date.from(Instant.now()));
        messages.add(message);

        return message.getId();
    }

    public int sendMessage(Message message, User sender, Group group) throws DataNotFoundException {
        //checking if the group exists
        Group groupFromDb = WhatsappValidationUtils.doesGroupExist(group.getName(), groups);


        List<User> userInGroup = groupUsers.get(groupFromDb);

        //checking if the sender is in the group
        User senderFromDb = mobileUsers.get(sender.getMobile());
        if (!userInGroup.contains(senderFromDb)) {
            throw new DataNotFoundException("You are not allowed to send message");
        }

        //checking if the message is not empty
        var messageFromDb = messages.stream()
                .filter(i -> i.getId() == message.getId())
                .findFirst()
                .orElseThrow(() -> new DataNotFoundException("Message not found"));


        //getting all message of a user
        var messagesInUser = userMessages.getOrDefault(senderFromDb, new ArrayList<>());
        //updating messageInUser
        messagesInUser.add(messageFromDb);
        //adding message to userMessages
        userMessages.put(senderFromDb, messagesInUser);


        //getting all messages of a groups
        List<Message> messagesInGroup = groupMessages.getOrDefault(groupFromDb, new ArrayList<>());
        //adding messages to groupMap
        messagesInGroup.add(messageFromDb);
        //updating the groupMap
        groupMessages.put(groupFromDb, messagesInGroup);

        return messagesInGroup.size();
    }

    public String changeAdmin(User approver, User user, Group group) throws DataNotFoundException {
        //checking if the group exists
        Group groupFromDb = WhatsappValidationUtils.doesGroupExist(group.getName(), groups);

        //checking if the approver is admin
        User approverFromDb = WhatsappValidationUtils.doesUserExist(approver.getMobile(), users);
        User userFromDb = WhatsappValidationUtils.doesUserExist(user.getMobile(), users);

        var usersInGroup = groupUsers.get(groupFromDb);
        if (!usersInGroup.contains(userFromDb) ) {
            throw new DataNotFoundException("User is not a participant");
        }

        if (!usersInGroup.get(0).equals(approverFromDb)) {
            throw new DataNotFoundException("Approver does not have rights");
        }

        usersInGroup.add(0, userFromDb);
        return "SUCCESS";
    }

    public int removeUser(User user) throws DataNotFoundException {
        //getting user from db
        User userFromdb = mobileUsers.getOrDefault(user.getMobile(), null);
        if (userFromdb == null) {
            throw new DataNotFoundException("User not found");
        }

        //getting group of a user
        Group group = null;
        for(var g : groupUsers.keySet()) {
            if (groupUsers.get(g).contains(userFromdb)) {
                group = g;
                break;
            }
        }

        if (group == null) {
            throw new DataNotFoundException("User not found");
        }

        var usersInGroup = groupUsers.get(group);

        if (usersInGroup.get(0).equals(userFromdb)) {
            throw new DataNotFoundException("Cannot remove admin");
        }

        //removing user from group
        //remove from users
        users.remove(userFromdb);
        mobileUsers.remove(userFromdb.getMobile());

        //removing all messages from userMessages
        var messagesByX = userMessages.get(userFromdb);
        userMessages.remove(userFromdb);

        //removing all mesaages of user from messages
        messages.removeAll(messagesByX);

        //modyfying the group
        group.setNumberOfParticipants(group.getNumberOfParticipants() - 1);

        //removing all messages of user from groupMessages
        var messagesByGroup = groupMessages.get(group);
        messagesByGroup.removeAll(messagesByX);
        groupMessages.put(group,  messagesByGroup);

        //updating the group everywhere
        for(var g : groups) {
            if (g.getName().equals(group.getName())) {
                g.setNumberOfParticipants(group.getNumberOfParticipants());
                break;
            }
        }
        for(var g : groupMessages.keySet()) {
            if (g.getName().equals(group.getName())) {
                g.setNumberOfParticipants(group.getNumberOfParticipants());
                break;
            }
        }
        for(var g : groupUsers.keySet()) {
            if (g.getName().equals(group.getName())) {
                g.setNumberOfParticipants(group.getNumberOfParticipants());
                break;
            }
        }
        return group.getNumberOfParticipants() + groupMessages.get(group).size() + messages.size();
    }

    public String findMessage(Date start, Date end, int k) {
        return messages.stream()
                .filter(i -> i.getTimestamp().after(start) && i.getTimestamp().before(end))
                .sorted(Comparator.comparing(Message::getTimestamp).reversed())
                .limit(k)
                .collect(Collectors.toList())
                .get(k - 1)
                .getContent();
    }
}
