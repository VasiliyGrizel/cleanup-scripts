import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.CustomFieldManager
import com.atlassian.jira.issue.fields.CustomField
import com.atlassian.jira.issue.IssueManager
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.MutableIssue
import org.apache.log4j.Logger
import org.apache.log4j.Level
import com.atlassian.jira.event.type.*
import com.atlassian.jira.util.ImportUtils
import com.atlassian.jira.issue.ModifiedValue
import com.atlassian.jira.issue.util.DefaultIssueChangeHolder
import com.atlassian.jira.issue.index.IssueIndexingService
import com.atlassian.jira.bc.issue.search.SearchService
import com.atlassian.jira.bc.issue.search.SearchService.ParseResult
import com.atlassian.jira.web.bean.PagerFilter


final def log = Logger.getLogger("com.gonchik.scripts.groovy.migrateToComment")
log.setLevel(Level.DEBUG)


String jqlSearch = 'project = test'
long toField = 000000
def user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()
IssueManager issueManager = ComponentAccessor.getIssueManager()
SearchService searchService = ComponentAccessor.getComponent(SearchService.class)
SearchService.ParseResult parseResult = searchService.parseQuery(user, jqlSearch)
CustomFieldManager customerFieldManager = ComponentAccessor.getCustomFieldManager()
CustomField toCustomField = customerFieldManager.getCustomFieldObject(toField)

def issueIndexingService = ComponentAccessor.getComponent(IssueIndexingService.class)

// does JQL valid?
if (!parseResult.isValid()) {
    return "Please, validate a JQL"
}

def searchResult = searchService.search(user, parseResult.getQuery(), PagerFilter.getUnlimitedFilter())
def issues = searchResult.results.collect { issueManager.getIssueObject(it.id) }

for (issue in issues) {
    def availableOptions = ComponentAccessor.optionsManager.getOptions(toCustomField.getRelevantConfig(issue))
	def value = availableOptions.find { it.value == "NO" }
    toCustomField.updateValue(null, issue, new ModifiedValue(null, value), new DefaultIssueChangeHolder())
    boolean wasIndexing = ImportUtils.isIndexIssues()
    ImportUtils.setIndexIssues(true)
    issueIndexingService.reIndex(issue)
}
def line = "Script successfully migrated data  for ${issues.size()} issues"
log.debug line

return line
