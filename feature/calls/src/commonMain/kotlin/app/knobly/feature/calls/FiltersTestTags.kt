package app.knobly.feature.calls

/** Test tags of the Filters tab and the rule editor, shared with UI tests. */
object FiltersTestTags {
    const val CONTACTS_ONLY = "filters.contactsOnly"
    const val CUSTOM_RULES = "filters.customRules"
    const val NEW_RULE = "filters.newRule"
    const val CONTACTS_PERMISSION_MESSAGE = "filters.contactsPermissionMessage"

    fun rule(id: String) = "filters.rule.$id"

    fun ruleToggle(id: String) = "filters.ruleToggle.$id"

    const val EDITOR_VALUE = "ruleEditor.value"
    const val EDITOR_LABEL = "ruleEditor.label"
    const val EDITOR_TEST = "ruleEditor.test"
    const val EDITOR_TEST_RESULT = "ruleEditor.testResult"
    const val EDITOR_SAVE = "ruleEditor.save"
    const val EDITOR_DELETE = "ruleEditor.delete"

    fun editorType(type: String) = "ruleEditor.type.$type"
}
