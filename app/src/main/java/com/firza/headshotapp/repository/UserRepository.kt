package com.firza.headshotapp.repository

import androidx.lifecycle.LiveData
import com.firza.headshotapp.db.dao.UserDao
import com.firza.headshotapp.db.entity.UserEntity

class UserRepository(private val userDao: UserDao) {
    suspend fun getAllUsers(): List<UserEntity> = userDao.getAllUsers()
    suspend fun insert(user: UserEntity) {
        userDao.insertUser(user)
    }

    suspend fun updateUser(user: UserEntity) {
        userDao.updateUser(user)
    }

    suspend fun deleteUser(userId: Int) {
        userDao.deleteUser(userId)
    }
}
