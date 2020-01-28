package me.theforbiddenai.jenkinsparserkotlin

interface Jenkins {

    fun search(query: String): List<Information>

}