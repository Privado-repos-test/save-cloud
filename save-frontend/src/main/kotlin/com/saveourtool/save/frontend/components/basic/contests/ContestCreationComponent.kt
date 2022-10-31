@file:Suppress("FILE_NAME_MATCH_CLASS", "FILE_WILDCARD_IMPORTS", "LargeClass")

package com.saveourtool.save.frontend.components.basic.contests

import com.saveourtool.save.entities.ContestDto
import com.saveourtool.save.frontend.components.basic.*
import com.saveourtool.save.frontend.components.basic.testsuiteselector.showContestTestSuitesSelectorModal
import com.saveourtool.save.frontend.components.inputform.*
import com.saveourtool.save.frontend.components.inputform.inputTextDisabled
import com.saveourtool.save.frontend.components.inputform.inputTextFormRequired
import com.saveourtool.save.frontend.components.modal.modal
import com.saveourtool.save.frontend.externals.modal.CssProperties
import com.saveourtool.save.frontend.externals.modal.Styles
import com.saveourtool.save.frontend.utils.*
import com.saveourtool.save.frontend.utils.noopLoadingHandler
import com.saveourtool.save.testsuite.TestSuiteDto
import com.saveourtool.save.utils.LocalDateTime
import com.saveourtool.save.validation.FrontendRoutes
import com.saveourtool.save.validation.isValidName

import csstype.ClassName
import org.w3c.fetch.Response
import react.*
import react.dom.html.ButtonType
import react.dom.html.ReactHTML.button
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.form

import kotlin.js.json
import kotlinx.datetime.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Component that allows to create new contests
 */
val contestCreationComponent = contestCreationComponent()

private val contestCreationCard = cardComponent()

/**
 *  Contest creation component props
 */
external interface ContestCreationComponentProps : Props {
    /**
     * Name of current organization
     */
    var organizationName: String

    /**
     * Callback invoked on successful contest creation
     */
    var onSaveSuccess: (String) -> Unit

    /**
     * Callback invoked on error while contest creation
     */
    var onSaveError: (Response) -> Unit
}

/**
 * @param organizationName name of an organization to which a new contest will be linked
 * @param isOpen flag that indicates if the modal is open
 * @param onSuccess callback invoked on successful contest creation
 * @param onFailure callback invoked on error when creating contest
 * @param onClose callback invoked on close button press
 */
fun ChildrenBuilder.showContestCreationModal(
    organizationName: String,
    isOpen: Boolean,
    onSuccess: (String) -> Unit,
    onFailure: (Response) -> Unit,
    onClose: () -> Unit,
) {
    modal { props ->
        props.isOpen = isOpen
        props.style = Styles(
            content = json(
                "top" to "15%",
                "left" to "30%",
                "right" to "30%",
                "bottom" to "auto",
                "position" to "absolute",
                "overflow" to "hide"
            ).unsafeCast<CssProperties>()
        )
        contestCreationComponent {
            this.organizationName = organizationName
            onSaveSuccess = onSuccess
            onSaveError = onFailure
        }
        div {
            className = ClassName("d-flex justify-content-center")
            button {
                type = ButtonType.button
                className = ClassName("btn btn-secondary mt-4")
                +"Cancel"
                onClick = {
                    onClose()
                }
            }
        }
    }
}

private fun String.dateStringToLocalDateTime(time: LocalTime = LocalTime(0, 0, 0)) = LocalDateTime(
    LocalDate.parse(this),
    time,
)

/**
 * @param startTime
 * @param endTime
 */
fun isDateRangeValid(startTime: LocalDateTime?, endTime: LocalDateTime?) = if (startTime != null && endTime != null) {
    startTime < endTime
} else {
    true
}

private fun isButtonDisabled(contestDto: ContestDto) = contestDto.endTime == null || contestDto.startTime == null || !isDateRangeValid(contestDto.startTime, contestDto.endTime) ||
        !contestDto.name.isValidName() || contestDto.testSuiteIds.isEmpty()

@Suppress(
    "TOO_LONG_FUNCTION",
    "LongMethod",
    "MAGIC_NUMBER",
    "AVOID_NULL_CHECKS"
)
private fun contestCreationComponent() = FC<ContestCreationComponentProps> { props ->
    val (contestDto, setContestDto) = useState(ContestDto.empty.copy(organizationName = props.organizationName))

    val (conflictErrorMessage, setConflictErrorMessage) = useState<String?>(null)

    val onSaveButtonPressed = useDeferredRequest {
        val response = post(
            "$apiUrl/${FrontendRoutes.CONTESTS.path}/create",
            jsonHeaders,
            Json.encodeToString(contestDto),
            ::noopLoadingHandler,
            ::responseHandlerWithValidation
        )
        if (response.ok) {
            props.onSaveSuccess("/${FrontendRoutes.CONTESTS.path}/${contestDto.name}")
        } else if (response.isConflict()) {
            setConflictErrorMessage(response.unpackMessage())
        } else {
            props.onSaveError(response)
        }
    }

    // fixme: can be removed after https://github.com/saveourtool/save-cloud/issues/1192
    val (testSuites, setTestSuites) = useState(emptyList<TestSuiteDto>())
    useRequest {
        val testSuitesFromBackend: List<TestSuiteDto> = post(
            url = "$apiUrl/test-suites/${contestDto.organizationName}/get-by-ids",
            headers = jsonHeaders,
            body = Json.encodeToString(contestDto.testSuiteIds),
            loadingHandler = ::loadingHandler,
            responseHandler = ::noopResponseHandler,
        )
            .decodeFromJsonString()

        setTestSuites(testSuitesFromBackend)
    }

    val testSuitesSelectorWindowOpenness = useWindowOpenness()
    div {
        className = ClassName("card")
        contestCreationCard {
            showContestTestSuitesSelectorModal(
                contestDto.organizationName,
                testSuites,
                testSuitesSelectorWindowOpenness,
                useState(emptyList()),
            ) { testSuiteDtos ->
                setContestDto(contestDto.copy(testSuiteIds = testSuiteDtos.map { it.requiredId() }))
                setTestSuites(testSuiteDtos)
            }
            div {
                className = ClassName("")
                form {
                    className = ClassName("needs-validation")
                    // ==== Contest Name
                    div {
                        className = ClassName("mt-2")
                        inputTextFormRequired {
                            form = InputTypes.CONTEST_NAME
                            textValue = contestDto.name
                            validInput = (contestDto.name.isBlank() || contestDto.name.isValidName()) && conflictErrorMessage == null
                            classes = "col-12 pl-2 pr-2"
                            name = "Contest name"
                            conflictMessage = conflictErrorMessage
                            onChangeFun = {
                                setContestDto(contestDto.copy(name = it.target.value))
                                setConflictErrorMessage(null)
                            }
                        }
                    }
                    // ==== Organization Name selection
                    div {
                        className = ClassName("mt-2")
                        inputTextDisabled(
                            InputTypes.CONTEST_SUPER_ORGANIZATION_NAME,
                            "col-12 pl-2 pr-2",
                            "Super organization name",
                            contestDto.organizationName,
                        )
                    }
                    // ==== Contest dates
                    div {
                        className = ClassName("mt-2 d-flex justify-content-between")
                        inputDateFormRequired(
                            InputTypes.CONTEST_START_TIME,
                            isDateRangeValid(contestDto.startTime, contestDto.endTime),
                            "col-6 pl-2",
                            "Starting time",
                        ) {
                            setContestDto(contestDto.copy(startTime = it.target.value.dateStringToLocalDateTime()))
                        }
                        inputDateFormRequired(
                            InputTypes.CONTEST_END_TIME,
                            isDateRangeValid(contestDto.startTime, contestDto.endTime),
                            "col-6 pr-2",
                            "Ending time",
                        ) {
                            setContestDto(contestDto.copy(endTime = it.target.value.dateStringToLocalDateTime(LocalTime(23, 59, 59))))
                        }
                    }
                    // ==== Contest test suites
                    div {
                        className = ClassName("mt-2")
                        inputTextFormRequired {
                            form = InputTypes.CONTEST_TEST_SUITE_IDS
                            conflictMessage = null
                            textValue = testSuites.map { it.name }.sorted().joinToString(", ")
                            validInput = true
                            classes = "col-12 pl-2 pr-2 text-center"
                            name = "Test Suites:"
                            onClickFun = testSuitesSelectorWindowOpenness.openWindowAction()
                        }
                    }
                    // ==== Contest description
                    div {
                        className = ClassName("mt-2")
                        inputTextFormOptional {
                            form = InputTypes.CONTEST_DESCRIPTION
                            textValue = contestDto.description
                            classes = "col-12 pl-2 pr-2"
                            name = "Contest description"
                            onChangeFun = {
                                setContestDto(contestDto.copy(description = it.target.value))
                            }
                        }
                    }
                }
            }
            div {
                className = ClassName("mt-3 d-flex justify-content-center")
                button {
                    type = ButtonType.button
                    className = ClassName("btn btn-primary")
                    disabled = isButtonDisabled(contestDto) || conflictErrorMessage != null
                    onClick = { onSaveButtonPressed() }
                    +"Create contest"
                }
            }
            conflictErrorMessage?.let {
                div {
                    className = ClassName("invalid-feedback d-block text-center")
                    +it
                }
            }
        }
    }
}
