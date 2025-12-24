# next_bus_ggi.py
import pandas as pd
from datetime import datetime, date, time, timedelta

EXCEL_PATH = "GGI.xlsx"  # put the file in same folder or update path

# Known stop names in the sheet — tweak if your sheet uses slightly different names
STOP_KEYS = ["Al Nahda", "Mamzar", "GGI", "GGI - R15", "GGI-R15", "GGI R15"]

def normalize_time_str(s):
    """Normalize time-like strings: '13;30' -> '13:30', strip spaces."""
    if pd.isna(s):
        return None
    s = str(s).strip()
    s = s.replace(";", ":")
    # sometimes times are like '05:15:00' or '05:15'
    return s

def parse_sheet_try_simple(df):
    """
    Best-effort parsing tailored to the structure discovered earlier:
    we assume the sheet contains a small block where rows represent:
    [Start location, Stop 1, Stop 2, Stop1(return), Start(return)]
    and columns after a few header rows are Bus times.
    """
    # convert everything to string for searching
    str_df = df.fillna("").astype(str)

    # Try to find the row index that contains 'Depature' or 'Depature Location' or 'Depature' misspelling
    header_row_idx = None
    for i, row in str_df.iterrows():
        row_text = " ".join(row.values).lower()
        if "depatur" in row_text or "depature" in row_text or "departure" in row_text or "depature location" in row_text:
            header_row_idx = i
            break
    # Fallback: search for row containing 'Start' and 'Stop 1'
    if header_row_idx is None:
        for i, row in str_df.iterrows():
            if "start" in row.str.lower().values and ("stop 1" in row.str.lower().values or "stop1" in row.str.lower().values):
                header_row_idx = i
                break

    # If still not found, make a best guess: use rows 2..6 (0-based) like the earlier exploration
    if header_row_idx is None:
        header_row_idx = 2

    # We'll attempt to take the next 5 rows as the stop times block
    block_start = header_row_idx + 1
    block_end = block_start + 5  # exclusive
    block = df.iloc[block_start:block_end, :].copy()

    # Try to determine the row labels (like Al Nahda, Mamzar, GGI, Mamzar, Al Nahda)
    row_labels = []
    for _, row in block.iterrows():
        # find any cell that seems like a location (match STOP_KEYS or 'Al Nahda','Mamzar','GGI')
        found = None
        for cell in row.values:
            if any(k.lower() in str(cell).lower() for k in ["al nahda", "mamzar", "ggi"]):
                found = str(cell)
                break
        row_labels.append(found if found is not None else "") 

    # If row_labels are empty, use default labels consistent with the route loop
    if not any("Al Nahda".lower() in (lbl or "").lower() or "Mamzar".lower() in (lbl or "").lower() or "GGI" in (lbl or "").upper() for lbl in row_labels):
        row_labels = ["Al Nahda Start", "Mamzar Stop1", "GGI Stop2", "Mamzar Return", "Al Nahda Return"]
    else:
        # Map detected label strings to consistent canonical labels if possible
        canonical = []
        for lbl in row_labels:
            low = (lbl or "").lower()
            if "al nahda" in low:
                canonical.append("Al Nahda")
            elif "mamzar" in low:
                # decide outbound or return later by position
                canonical.append("Mamzar")
            elif "ggi" in low:
                canonical.append("GGI")
            else:
                canonical.append(lbl or "")
        # Expand to five expected rows if fewer detected
        while len(canonical) < 5:
            canonical.append("")
        # Give final canonical names with distinct markers for inbound/outbound later
        row_labels = ["Al Nahda Start", "Mamzar Stop1", "GGI Stop2", "Mamzar Return", "Al Nahda Return"]

    # Now extract times for each column (buses are columns)
    # We skip columns that are empty in the block
    times_by_bus = {}
    for col in block.columns:
        col_vals = block[col].tolist()
        # normalize strings
        col_vals_norm = [normalize_time_str(v) for v in col_vals]
        # if at least one cell looks like a time, treat this column as a bus column
        if any(v and any(ch.isdigit() for ch in v) for v in col_vals_norm):
            # convert to parsed times where possible
            parsed = []
            for v in col_vals_norm:
                if not v:
                    parsed.append(None)
                else:
                    # try multiple formats
                    parsed_time = None
                    for fmt in ("%H:%M:%S", "%H:%M", "%I:%M %p", "%I:%M:%S %p"):
                        try:
                            parsed_time = datetime.strptime(v, fmt).time()
                            break
                        except:
                            parsed_time = None
                    parsed.append(parsed_time)
            times_by_bus[str(col)] = dict(zip(row_labels, parsed))

    # Build a tidy dataframe: each row = (Bus, StopLabel, Time)
    rows = []
    for bus, mapping in times_by_bus.items():
        for stop_label, t in mapping.items():
            if t is not None:
                # map stop_label to canonical stop names (Mamzar, Al Nahda, GGI)
                if "Mamzar" in stop_label:
                    stop = "Mamzar"
                elif "Al Nahda" in stop_label:
                    stop = "Al Nahda"
                elif "GGI" in stop_label or "R15" in stop_label.upper():
                    stop = "GGI"
                else:
                    stop = stop_label
                rows.append({"Bus": bus, "StopLabel": stop_label, "Stop": stop, "Time": t})
    tidy = pd.DataFrame(rows)
    return tidy

def find_next_bus(tidy_df, current_location, dest_location, current_time_obj):
    """
    tidy_df: DataFrame with columns [Bus, StopLabel, Stop, Time]
    current_location/dest_location: strings (e.g. "Mamzar", "GGI", "Al Nahda")
    current_time_obj: datetime.time
    """
    # Candidate arrivals at the current stop in chronological order
    candidates = tidy_df[tidy_df["Stop"].str.lower() == current_location.lower()].copy()
    # ensure times are actual time objects
    candidates = candidates.dropna(subset=["Time"])
    # find times strictly after current_time (next arrivals)
    candidates["after_now"] = candidates["Time"].apply(lambda t: t > current_time_obj)
    upcoming = candidates[candidates["after_now"]].copy().sort_values("Time")
    if upcoming.empty:
        return None  # no more buses today
    # Pick the earliest upcoming arrival
    next_row = upcoming.iloc[0]
    bus = next_row["Bus"]
    arrival_time = next_row["Time"]

    # Determine direction: check that this bus later has an entry at dest_location (after this stop)
    # Find times for same bus at all stops and sort in the expected loop order by time.
    bus_schedule = tidy_df[tidy_df["Bus"] == bus].copy().sort_values("Time")
    # If the destination stop appears later in time than the current stop, it's heading toward dest
    cur_time = arrival_time
    dest_entries = bus_schedule[bus_schedule["Stop"].str.lower() == dest_location.lower()]
    dest_after = dest_entries[dest_entries["Time"] > cur_time]
    if not dest_after.empty:
        direction = f"toward {dest_location}"
    else:
        # if dest appears before current stop in this bus instance, then direction is opposite
        direction = f"away from {dest_location} (or return leg)"
    # time remaining in minutes
    now_dt = datetime.combine(date.today(), current_time_obj)
    arr_dt = datetime.combine(date.today(), arrival_time)
    delta_min = int((arr_dt - now_dt).total_seconds() // 60)
    return {"Bus": bus, "ArrivalTime": arrival_time.strftime("%H:%M"), "Minutes": delta_min, "Direction": direction}

def main_example():
    # Load raw excel
    df = pd.read_excel(EXCEL_PATH, header=None)
    tidy = parse_sheet_try_simple(df)

    # Example test from your message:
    current_time = datetime.strptime("20:48", "%H:%M").time()
    cur_loc = "Mamzar"
    dest = "GGI"
    result = find_next_bus(tidy, cur_loc, dest, current_time)
    if result is None:
        print("No more buses today from", cur_loc)
    else:
        print(f"Next bus from {cur_loc} to {dest}:")
        print(f" Bus column: {result['Bus']}")
        print(f" Arrival time at {cur_loc}: {result['ArrivalTime']} (in {result['Minutes']} minutes)")
        print(f" Direction: {result['Direction']}")

if __name__ == "__main__":
    main_example()
