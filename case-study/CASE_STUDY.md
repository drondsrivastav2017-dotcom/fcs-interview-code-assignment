# Case Study Scenarios to discuss

## Scenario 1: Cost Allocation and Tracking
**Situation**: The company needs to track and allocate costs accurately across different Warehouses and Stores. The costs include labor, inventory, transportation, and overhead expenses.

**Task**: Discuss the challenges in accurately tracking and allocating costs in a fulfillment environment. Think about what are important considerations for this, what are previous experiences that you have you could related to this problem and elaborate some questions and considerations

**Questions you may have and considerations:**

**Questions I would ask before scoping the work**

- What decision should this data support? "Which warehouses are unprofitable", "should we keep this store open" and "what does it cost to fulfil one order" are three different systems. The answer sets the required granularity, and granularity is what drives most of the cost of the build.
- What is the cost object? Per warehouse and per store is the easy answer, but the useful one is usually per unit fulfilled, per order line, or per product family within a location. Which of those does Finance already use?
- Which costs are direct and which are shared? Labour and transportation are largely traceable; overhead (rent, systems, management) is not, and must be allocated with a driver everybody agrees on.
- Who owns the allocation rules, and how often do they change? If Finance changes the driver every quarter, the rules cannot be hardcoded.
- What is the source of truth for each cost type, at what frequency does it arrive, and how late can it be? Payroll is monthly, carrier invoices arrive weeks later, warehouse labour may be daily.
- What accuracy is good enough? Reporting within a few percent is a very different (and much cheaper) system than one feeding statutory accounts.

**Considerations**

- *Allocation is a business agreement, not a technical problem.* The hard part is not computing the numbers, it is getting Operations and Finance to agree on the drivers - square metres, units shipped, labour hours, order lines. I would model allocation rules as data with an effective date, never as code, so a new rule does not need a release and history stays reproducible.
- *Cost data arrives late and is corrected.* Any model that assumes immutable facts breaks on the first carrier credit note. I would separate the operational fact (a shipment happened) from the financial fact (it cost X, later corrected to Y), keep both append-only, and restate by version rather than by overwrite - otherwise last month's report changes silently and nobody trusts the tool again.
- *Time granularity and dimensional stability.* Costs must be attributable to a period and to a business unit that still means the same thing over time. This is exactly why the Business Unit Code in this domain matters: it is the stable dimension key across warehouse replacements (see Scenario 5).
- *Traceability over precision.* A number that cannot be explained is worse than an approximate number that can be drilled down to its source. Every allocated figure should be traceable to the raw cost record and to the rule version that produced it.
- *Start narrow.* One cost type (for instance warehouse labour), one location, an agreed driver, and a report that Finance validates against their own numbers. Extending a model that is trusted is easy; recovering from a tool that produced wrong numbers in month one is not.

## Scenario 2: Cost Optimization Strategies
**Situation**: The company wants to identify and implement cost optimization strategies for its fulfillment operations. The goal is to reduce overall costs without compromising service quality.

**Task**: Discuss potential cost optimization strategies for fulfillment operations and expected outcomes from that. How would you identify, prioritize and implement these strategies?

**Questions you may have and considerations:**

**Questions I would ask before scoping the work**

- What is the cost baseline per unit today, and how does it vary across locations? Optimisation without a baseline is guesswork, and the variance between similar sites is usually where the money is.
- What is the service level we must not compromise - delivery promise, fill rate, damage rate? A target that is not measured will be traded away silently.
- Which costs are actually controllable in the short term? Rent and contracts are fixed for years; labour scheduling, replenishment policy and warehouse-to-store assignment are not.
- What is the planning horizon and who owns each lever? A network redesign is a yearly decision; slotting and staffing are weekly.
- Is there capacity pressure or slack in the network right now? That determines whether the opportunity is consolidation or better utilisation.

**Considerations**

- *Typical levers, roughly in order of effort.* (1) Assignment and sourcing: which warehouse fulfils which product for which store - the closest fit to what this system already models, and the cheapest lever to pull, since it is a data change rather than a physical one. (2) Capacity utilisation: a warehouse running at 30% of its capacity carries overhead per unit that no labour efficiency will recover. (3) Labour: scheduling against forecast demand instead of a fixed roster. (4) Transportation: consolidation, fewer and fuller trips, route and carrier mix. (5) Inventory placement: less stock in the wrong place means less transfer and less obsolescence. (6) Network structure: opening, closing or replacing a location - highest impact, slowest and riskiest.
- *How I would identify and prioritise.* Start from the cost per unit by location and by cost type from Scenario 1, and look for variance rather than absolute size: two similar warehouses with different unit costs is a concrete, provable opportunity. Then rank candidates on expected annual saving, time to realise, implementation cost and service risk, and be explicit that a 2% saving that risks the delivery promise is not a saving.
- *Expected outcomes must be stated up front.* Every initiative gets a hypothesis, a metric, a target and a review date. Without that, results are attributed to whoever speaks loudest, and the same initiative gets proposed again a year later.
- *Implement incrementally and measure honestly.* Pilot on one location or one product family, keep a comparable control group, and account for seasonality - fulfilment costs move with volume, so a January improvement may be nothing more than January. Beware local optimisation: reducing transport by shipping full trucks less frequently raises store stockouts, and the cost moves rather than disappears.
- *The system's role.* The tool should make the levers visible and measurable and the effect of a decision auditable. Automated optimisation is a later step, and only worth it once people trust the numbers.

## Scenario 3: Integration with Financial Systems
**Situation**: The Cost Control Tool needs to integrate with existing financial systems to ensure accurate and timely cost data. The integration should support real-time data synchronization and reporting.

**Task**: Discuss the importance of integrating the Cost Control Tool with financial systems. What benefits the company would have from that and how would you ensure seamless integration and data synchronization?

**Questions you may have and considerations:**

**Questions I would ask before scoping the work**

- Which system is the source of truth for each piece of data - the ERP for actual costs, this tool for operational allocation? Two systems both claiming truth is the most common and most expensive failure mode.
- What does "real-time" mean for the actual decisions? Costs are usually consumed daily or monthly; the requirement is often freshness and reliability, not milliseconds. The two have very different price tags.
- What integration surface does the financial system actually offer - API, file export, database, message queue - and what is its change cadence and release process?
- How are the shared dimensions mastered: cost centres, GL accounts, business unit codes, calendars and currencies? Mapping tables that nobody owns are where reconciliation goes to die.
- What is the period-close process, and what happens to data that arrives after the books are closed?
- What are the audit, retention and segregation-of-duties requirements? Financial data raises the compliance bar for the whole system.

**Considerations**

- *Benefits worth naming.* One reconciled set of numbers instead of competing spreadsheets; operational decisions and financial reporting that agree; less manual re-keying and the errors that come with it; and faster close, because allocation is continuous instead of a month-end scramble.
- *Direction and ownership.* I would define one clear direction per data element and one owner, and avoid bidirectional synchronisation wherever possible: the ERP owns actual costs, the Cost Control Tool owns operational allocation and sends journal-ready output back.
- *Event-driven where it helps, batch where it is honest.* Operational events (a warehouse activated, a replacement performed) are naturally events and should be published as such - the transactional outbox pattern is the right tool, which is exactly the problem solved in task 2 of the code assignment, where the legacy system is only notified after the database transaction commits. Financial postings, in contrast, are periodic and are better served by a controlled batch with a clear cut-off.
- *Design for failure, because the other system will be down.* Idempotent consumers, retries with backoff, a dead-letter path and an alert when the lag exceeds a threshold. "Fire and forget" against a finance system becomes a silent data gap discovered at close.
- *Reconciliation is a feature, not an afterthought.* A scheduled control that compares totals per period and per dimension between both systems, with a visible report of the differences. Without it, nobody can answer "is the tool right?" and the answer defaults to "no".
- *Stable contracts.* Versioned, explicitly specified interfaces (the spec-first argument from question 2 applies directly here), plus contract tests in CI, because the financial system will be upgraded on a schedule you do not control.

## Scenario 4: Budgeting and Forecasting
**Situation**: The company needs to develop budgeting and forecasting capabilities for its fulfillment operations. The goal is to predict future costs and allocate resources effectively.

**Task**: Discuss the importance of budgeting and forecasting in fulfillment operations and what would you take into account designing a system to support accurate budgeting and forecasting?

**Questions you may have and considerations:**

**Questions I would ask before scoping the work**

- What decisions depend on the forecast - headcount, capacity commitments, carrier contracts, opening or closing a location? The decision determines the horizon and the accuracy that is actually needed.
- What granularity and horizon: weekly per warehouse for staffing, monthly per business unit for the budget, yearly for the network plan?
- Is there a demand forecast already, and who owns it? Cost forecasting is mostly demand forecasting plus a cost model; building a second, competing demand forecast is a classic waste.
- How much history is available, and is it comparable? Two years of history across a network that has been restructured is not two years of usable history.
- Who approves the budget, and how are re-forecasts and versions handled during the year?
- What accuracy is expected, and how will it be measured? Without a defined error metric, every forecast is "wrong" in hindsight.

**Considerations**

- *Why it matters operationally.* In fulfilment most costs are committed before the volume arrives: staff are hired and scheduled, space is leased, carriers are contracted. A forecast that is late or wrong is not a reporting problem, it is either paying for idle capacity or failing the delivery promise in peak season.
- *Separate the drivers from the rates.* Model volume (the driver) separately from cost per unit (the rate). Most forecast error comes from volume, while most of the controllable improvement comes from the rate, and mixing them hides both.
- *Fixed, variable and step costs.* Fulfilment cost is not linear: a warehouse absorbs volume until a second shift or a new site is needed, and then jumps. A purely linear model will be confidently wrong exactly at the decision points that matter, which is where capacity constraints like the ones modelled in this system (maximum number of warehouses and maximum capacity per location) become the input to the plan.
- *Seasonality and events.* Peaks, promotions and holidays dominate the profile. The model needs a calendar of known events, and the ability for a planner to override an assumption - with the override recorded and attributable, not silently merged in.
- *Scenarios, not a single number.* Budgeting is a conversation about "what if volume is 20% higher" or "what if this warehouse is replaced". The system should make scenarios cheap to create, compare and keep, versioned and immutable once approved.
- *Close the loop.* Store every forecast version and compare it against actuals by period and by dimension. Forecast accuracy tracked over time is what turns a model into a trusted tool, and it also reveals whether the error is in volume, in rate, or in the allocation rules themselves.
- *Start simple.* A transparent driver-based model that planners can explain beats a sophisticated statistical model that nobody trusts. Sophistication is earned once the basics reconcile.

## Scenario 5: Cost Control in Warehouse Replacement
**Situation**: The company is planning to replace an existing Warehouse with a new one. The new Warehouse will reuse the Business Unit Code of the old Warehouse. The old Warehouse will be archived, but its cost history must be preserved.

**Task**: Discuss the cost control aspects of replacing a Warehouse. Why is it important to preserve cost history and how this relates to keeping the new Warehouse operation within budget?

**Questions you may have and considerations:**

**Questions I would ask before scoping the work**

- Is the Business Unit Code a stable dimension for reporting, or does the replacement start a new cost object? The whole reporting continuity depends on this answer, and it must come from Finance, not from engineering.
- Is there an overlap period where both warehouses operate and both incur cost, and how should that double-running cost be treated - capitalised as part of the transition, or expensed as part of the business unit?
- What is the business case for the replacement, and which metric proves it: cost per unit, capacity, service level, or all three?
- Which one-off costs belong to the transition (moving stock, parallel running, ramp-up inefficiency) as opposed to the new warehouse's steady-state cost?
- How long is the ramp-up period before the new unit is expected to hit its target cost per unit?

**Considerations**

- *Why preserving history is the point of the exercise.* The business unit is the continuous entity; the physical warehouse is just its current implementation. If the history were archived away with the old row, the company would lose its only baseline and could no longer answer the question the replacement was justified with: "is the new warehouse actually cheaper per unit than the one it replaced?" This is exactly why the implemented replacement archives the previous warehouse instead of deleting it - `archivedAt` is set, the row and its `createdAt` are kept, and the new unit is created with the same business unit code. Reporting can therefore follow the business unit across the change, and still attribute each cost to the physical unit that incurred it.
- *Comparability requires context, not just continuity.* A cost series that silently changes meaning in the middle is worse than a broken one. The archival timestamps mark the boundary, and any comparison should be normalised per unit handled rather than in absolute terms, since capacity and volume usually change with the replacement.
- *Budget control during the transition.* The riskiest period is the overlap: two sets of fixed costs, stock in movement and a workforce that is still learning the new site. I would budget the transition explicitly and separately from steady-state operation, so that ramp-up inefficiency does not permanently contaminate the new unit's cost baseline, and so that the overrun is visible while it can still be acted on.
- *Set the target before the move, and check it afterwards.* The new unit inherits the history and therefore an expected cost per unit; the replacement should carry an explicit target and a date by which it is expected to be met, with a post-implementation review against the preserved baseline. Without that review, replacements get justified by a business case that nobody ever verifies.
- *Constraints are part of cost control.* The rules enforced at replacement time - capacity must accommodate the stock being transferred, stock must match, and the location must still respect its capacity and warehouse limits - are not bureaucratic checks: they prevent a replacement that looks cheaper on paper but leaves the network unable to absorb the same volume, which is a cost that would surface later as transfers, overtime and missed deliveries.

## Instructions for Candidates
Before starting the case study, read the [BRIEFING.md](BRIEFING.md) to quickly understand the domain, entities, business rules, and other relevant details.

**Analyze the Scenarios**: Carefully analyze each scenario and consider the tasks provided. To make informed decisions about the project's scope and ensure valuable outcomes, what key information would you seek to gather before defining the boundaries of the work? Your goal is to bridge technical aspects with business value, bringing a high level discussion; no need to deep dive.
