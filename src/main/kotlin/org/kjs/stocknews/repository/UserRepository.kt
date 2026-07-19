package org.kjs.stocknews.repository

import org.kjs.stocknews.model.table.User
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User, Long> {
    fun existsByEmail(email: String): Boolean
}
