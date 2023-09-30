package com.driver.validation;

import com.driver.Group;
import com.driver.User;
import com.driver.exception.DataNotFoundException;

import java.util.List;
import java.util.Objects;

public class WhatsappValidationUtils {

    public static Group doesGroupExist(String groupName, List<Group> groups) throws DataNotFoundException {
        Group groupFromDb = null;
        for (var dbg : groups) {
            if (dbg.getName().equals(groupName)) {
                groupFromDb = dbg;
                break;
            }
        }
        if (Objects.isNull(groupFromDb)) {
            throw new DataNotFoundException("Group does not exist");
        }
        return groupFromDb;
    }

    public static User doesUserExist(String mobile, List<User> users) throws DataNotFoundException {
        User userFromDb = null;
        for (var dbu : users) {
            if (dbu.getMobile().equals(mobile)) {
                userFromDb = dbu;
                break;
            }
        }
        if (Objects.isNull(userFromDb)) {
            throw new DataNotFoundException("User does not exist");
        }
        return userFromDb;
    }
}
