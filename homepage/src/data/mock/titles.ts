import type { ContentDetail, ContentType, Episode, Season } from '@/types/content';
import { castFor, personById } from './people';

/** Authoring shape — richer than ContentSummary; the provider projects down to it. */
export interface TitleRecord extends Omit<ContentDetail, 'cast' | 'director' | 'creators'> {
  castIds: string[];
  castRoles: string[];
  directorId?: string;
  creatorIds?: string[];
}

function art(key: string) {
  return { key };
}

function movie(r: Omit<TitleRecord, 'poster' | 'backdrop' | 'type' | 'sourceId' | 'trailerAvailable'> & {
  trailerAvailable?: boolean;
}): TitleRecord {
  return {
    ...r,
    type: 'movie' as ContentType,
    poster: art(`poster_${r.id}`),
    backdrop: art(`backdrop_${r.id}`),
    sourceId: 'mango.mock',
    trailerAvailable: r.trailerAvailable ?? true,
  } as unknown as TitleRecord;
}

function series(r: Omit<TitleRecord, 'poster' | 'backdrop' | 'type' | 'sourceId' | 'trailerAvailable'> & {
  trailerAvailable?: boolean;
}): TitleRecord {
  return {
    ...r,
    type: 'series' as ContentType,
    poster: art(`poster_${r.id}`),
    backdrop: art(`backdrop_${r.id}`),
    sourceId: 'mango.mock',
    trailerAvailable: r.trailerAvailable ?? true,
  } as unknown as TitleRecord;
}

function ep(
  titleId: string,
  season: number,
  number: number,
  title: string,
  synopsis: string,
  runtimeMinutes = 44,
  progress?: number,
): Episode {
  return {
    id: `${titleId}_s${season}e${number}`,
    seasonNumber: season,
    episodeNumber: number,
    title,
    synopsis,
    runtimeMinutes,
    still: art(`still_${titleId}_s${season}e${number}`),
    progress,
  };
}

function season(number: number, title: string, episodes: Episode[]): Season {
  return { seasonNumber: number, title, episodeCount: episodes.length, episodes };
}

export const MOVIES: TitleRecord[] = [
  movie({
    id: 'mv01',
    title: 'Ashfall Horizon',
    year: 2024,
    genres: ['action', 'thriller'],
    certification: '15',
    synopsis:
      'When a volcanic chain reaction cuts off a Pacific research outpost, its structural engineer has eleven hours to get thirty people off the island before the shelf collapses.',
    rating: 7.8,
    runtimeMinutes: 118,
    castIds: ['p04', 'p07', 'p12'],
    castRoles: ['Reyes Calder', 'Dr. Amina Bloom', 'Captain Voss'],
    directorId: 'p22',
  }),
  movie({
    id: 'mv02',
    title: 'The Quiet Ledger',
    year: 2023,
    genres: ['drama', 'crime'],
    certification: '15',
    synopsis:
      'A forensic accountant discovers her late father\'s firm laundered money for three decades — and that unraveling it will implicate the man who raised her.',
    rating: 8.1,
    runtimeMinutes: 131,
    castIds: ['p01', 'p14'],
    castRoles: ['Margot Lin', 'Elias Marsh'],
    directorId: 'p09',
  }),
  movie({
    id: 'mv03',
    title: 'Paper Moths',
    year: 2022,
    genres: ['romance', 'drama'],
    certification: '12',
    synopsis:
      'Two rival letterpress printers in a dying industrial town fall for each other one order at a time, and have to decide if love survives when one business has to close.',
    rating: 7.4,
    runtimeMinutes: 104,
    castIds: ['p11', 'p17'],
    castRoles: ['Josephine Duval', 'Callum Reyes'],
    directorId: 'p03',
  }),
  movie({
    id: 'mv04',
    title: 'Nine Levels Down',
    year: 2025,
    genres: ['sci-fi', 'thriller'],
    certification: '15',
    synopsis:
      "A maintenance technician on a generation ship discovers the lower nine decks were sealed off eighty years ago — and something down there is still transmitting.",
    rating: 8.3,
    runtimeMinutes: 122,
    castIds: ['p08', 'p19', 'p24'],
    castRoles: ['Tomasu Reyn', 'Ivy Castellan', 'Dr. Noor Haddad'],
    directorId: 'p06',
  }),
  movie({
    id: 'mv05',
    title: 'Last Orders at the Comet',
    year: 2021,
    genres: ['comedy', 'drama'],
    certification: '12',
    synopsis:
      "On the final night of a roadside diner that's fed the same three exits for fifty years, every regular who ever walked out on a tab comes back to settle up.",
    rating: 7.6,
    runtimeMinutes: 98,
    castIds: ['p16', 'p20', 'p05'],
    castRoles: ['Rosa Mendez', 'Malik Preston', 'Priya Anand'],
    directorId: 'p13',
  }),
  movie({
    id: 'mv06',
    title: 'The Cartographer’s Daughter',
    year: 2024,
    genres: ['adventure', 'drama'],
    certification: 'PG',
    synopsis:
      'Inheriting her father\'s unfinished survey of a border river, a young mapmaker follows his last route and finds the border was never where the maps say it is.',
    rating: 7.9,
    runtimeMinutes: 112,
    castIds: ['p23', 'p02'],
    castRoles: ['Talia Brandt', 'Theo Okafor'],
    directorId: 'p18',
  }),
  movie({
    id: 'mv07',
    title: 'Hollow Choir',
    year: 2023,
    genres: ['horror', 'thriller'],
    certification: '18',
    synopsis:
      'A restoration crew rebuilding a flooded church basement starts hearing a hymn nobody in the parish has sung in sixty years — always one voice short of a full choir.',
    rating: 7.2,
    runtimeMinutes: 101,
    castIds: ['p21', 'p14', 'p25'],
    castRoles: ['Vera Kaminski', 'Felix Draven', 'Miles Ashworth'],
    directorId: 'p07',
  }),
  movie({
    id: 'mv08',
    title: 'Signal to Noise',
    year: 2022,
    genres: ['thriller', 'drama'],
    certification: '15',
    synopsis:
      'A radio astronomer flags an anomalous signal the night before her funding is cut — and spends the next seventy-two hours proving it wasn’t a mistake before the array goes dark.',
    rating: 7.7,
    runtimeMinutes: 109,
    castIds: ['p19', 'p10'],
    castRoles: ['Ivy Castellan', 'Samuel Iyer'],
    directorId: 'p15',
  }),
  movie({
    id: 'mv09',
    title: 'Foxglove',
    year: 2020,
    genres: ['drama', 'romance'],
    certification: '12',
    synopsis:
      'A botanist returns to her grandmother\'s overgrown estate to catalogue it before the sale closes, and finds the garden was planted as a forty-year love letter.',
    rating: 7.3,
    runtimeMinutes: 106,
    castIds: ['p09', 'p12'],
    castRoles: ['Freya Lindqvist', 'Owen Blackwood'],
    directorId: 'p01',
  }),
  movie({
    id: 'mv10',
    title: 'Redline',
    year: 2025,
    genres: ['action'],
    certification: '15',
    synopsis:
      'A disgraced pit strategist gets one race to clear her name — and the only car fast enough to prove it belongs to the man who framed her.',
    rating: 7.5,
    runtimeMinutes: 114,
    castIds: ['p06', 'p16'],
    castRoles: ['Diego Fuentes', 'Rosa Herrera'],
    directorId: 'p04',
  }),
  movie({
    id: 'mv11',
    title: 'The Understudy',
    year: 2021,
    genres: ['drama', 'comedy'],
    certification: '12',
    synopsis:
      'Twelve years into covering the same role and never once going on, an understudy finally gets her shot the same week she\'d decided to quit theatre for good.',
    rating: 7.8,
    runtimeMinutes: 108,
    castIds: ['p23', 'p11'],
    castRoles: ['Talia Brandt', 'Clara Duval'],
    directorId: 'p03',
  }),
  movie({
    id: 'mv12',
    title: 'Deep Winter Salvage',
    year: 2023,
    genres: ['thriller', 'adventure'],
    certification: '15',
    synopsis:
      'A salvage diver takes a job stripping a sunken tanker before the ice closes the strait, not knowing the client\'s real cargo went down with the crew still aboard.',
    rating: 7.6,
    runtimeMinutes: 117,
    castIds: ['p17', 'p24'],
    castRoles: ['Callum Reyes', 'Noor Haddad'],
    directorId: 'p22',
  }),
  movie({
    id: 'mv13',
    title: 'Amaranth',
    year: 2024,
    genres: ['fantasy', 'adventure'],
    certification: '12',
    synopsis:
      'The last apprentice of a dissolved order of gardeners has to smuggle a seed vault across a burning kingdom before its ruling council salts the fields for good.',
    rating: 8.0,
    runtimeMinutes: 128,
    castIds: ['p13', 'p08', 'p05'],
    castRoles: ['Amara Nwosu', 'Hiro Tanaka', 'Priya Anand'],
    directorId: 'p18',
  }),
  movie({
    id: 'mv14',
    title: 'The Long Weigh-In',
    year: 2019,
    genres: ['comedy'],
    certification: '15',
    synopsis:
      'A washed-up boxing commentator gets talked into training a fighter half his age for one last title shot he\'s convinced is a scam — and slowly isn’t.',
    rating: 7.1,
    runtimeMinutes: 99,
    castIds: ['p20', 'p02'],
    castRoles: ['Malik Preston', 'Theo Okafor'],
    directorId: 'p13',
  }),
  movie({
    id: 'mv15',
    title: 'Static Bloom',
    year: 2022,
    genres: ['sci-fi', 'drama'],
    certification: '12',
    synopsis:
      'A synthetic caretaker assigned to a dying botanist starts keeping a private log of things it isn’t supposed to feel, in a house that’s slowly being sold off room by room.',
    rating: 8.2,
    runtimeMinutes: 116,
    castIds: ['p15', 'p01'],
    castRoles: ['Yuki Sato', 'Elena Marsh'],
    directorId: 'p09',
  }),
  movie({
    id: 'mv16',
    title: 'Crown Fire',
    year: 2020,
    genres: ['action', 'drama'],
    certification: '15',
    synopsis:
      'A wildfire smokejumper pulled from retirement for one impossible season has to hold a ridge alone long enough for a town that fired her to evacuate.',
    rating: 7.5,
    runtimeMinutes: 110,
    castIds: ['p07', 'p12'],
    castRoles: ['Naomi Cross', 'Owen Blackwood'],
    directorId: 'p04',
  }),
  movie({
    id: 'mv17',
    title: 'The Rehearsal Dinner',
    year: 2018,
    genres: ['comedy', 'romance'],
    certification: '12',
    synopsis:
      'A wedding planner realizes forty minutes before the rehearsal dinner that she’s in love with the best man, and has one very long night to decide what to do about it.',
    rating: 6.9,
    runtimeMinutes: 96,
    castIds: ['p16', 'p25'],
    castRoles: ['Rosa Herrera', 'Miles Ashworth'],
    directorId: 'p01',
  }),
  movie({
    id: 'mv18',
    title: 'Ironbark',
    year: 2023,
    genres: ['drama', 'crime'],
    certification: '15',
    synopsis:
      'A parole officer in a logging town has to choose between the system and the kid she’s supposed to be protecting it from, one broken curfew at a time.',
    rating: 7.9,
    runtimeMinutes: 121,
    castIds: ['p10', 'p21'],
    castRoles: ['Samuel Iyer', 'Vera Kaminski'],
    directorId: 'p14',
  }),
  movie({
    id: 'mv19',
    title: 'The Second Chair',
    year: 2021,
    genres: ['drama'],
    certification: '12',
    synopsis:
      'A second violinist who has spent twenty years exactly one seat from principal gets the call up the same week her mentor asks her to help him retire quietly.',
    rating: 7.7,
    runtimeMinutes: 103,
    castIds: ['p11', 'p22'],
    castRoles: ['Clara Duval', 'Adrian Voss'],
    directorId: 'p03',
  }),
  movie({
    id: 'mv20',
    title: 'Dry Season',
    year: 2020,
    genres: ['drama', 'thriller'],
    certification: '15',
    synopsis:
      'A water rights lawyer returns to the valley she grew up in to find the reservoir her family fought to build is now the reason her neighbors are leaving.',
    rating: 7.6,
    runtimeMinutes: 115,
    castIds: ['p09', 'p04'],
    castRoles: ['Freya Lindqvist', 'Marcus Webb'],
    directorId: 'p06',
  }),
  movie({
    id: 'mv21',
    title: 'Halfway to Ceres',
    year: 2025,
    genres: ['sci-fi', 'adventure'],
    certification: '12',
    synopsis:
      'A cargo hauler and her unlicensed co-pilot take a job too good to be legal, hauling something across the belt that three different fleets are now chasing.',
    rating: 7.8,
    runtimeMinutes: 124,
    castIds: ['p19', 'p06', 'p17'],
    castRoles: ['Ivy Castellan', 'Diego Fuentes', 'Callum Reyes'],
    directorId: 'p08',
  }),
  movie({
    id: 'mv22',
    title: 'The Wax Museum Job',
    year: 2019,
    genres: ['comedy', 'crime'],
    certification: '12',
    synopsis:
      'Four ex-con friends reunite for one absurd heist: stealing back a wax figure of themselves before it embarrasses the entire family at a museum gala.',
    rating: 6.8,
    runtimeMinutes: 93,
    castIds: ['p20', 'p05', 'p25', 'p16'],
    castRoles: ['Malik Preston', 'Priya Anand', 'Miles Ashworth', 'Rosa Herrera'],
    directorId: 'p13',
  }),
  movie({
    id: 'mv23',
    title: 'Undertow',
    year: 2022,
    genres: ['thriller', 'drama'],
    certification: '15',
    synopsis:
      "A coast guard rescue swimmer starts questioning every call from the same fishing fleet, certain the drownings are covering for something the boats are actually smuggling.",
    rating: 7.4,
    runtimeMinutes: 107,
    castIds: ['p12', 'p24'],
    castRoles: ['Owen Blackwood', 'Noor Haddad'],
    directorId: 'p22',
  }),
  movie({
    id: 'mv24',
    title: 'Marrow',
    year: 2024,
    genres: ['horror'],
    certification: '18',
    synopsis:
      "A hospice nurse takes a live-in post at a remote estate for a patient who insists he isn’t dying, in a house with a cellar the family never mentions.",
    rating: 7.3,
    runtimeMinutes: 99,
    castIds: ['p21', 'p14'],
    castRoles: ['Vera Kaminski', 'Felix Draven'],
    directorId: 'p07',
  }),
  movie({
    id: 'mv25',
    title: 'Small Mercies',
    year: 2018,
    genres: ['drama'],
    certification: '12',
    synopsis:
      'A prison chaplain and the detective who put half her congregation away have to work together when a former inmate goes missing on his first week out.',
    rating: 7.5,
    runtimeMinutes: 105,
    castIds: ['p13', 'p10'],
    castRoles: ['Amara Nwosu', 'Samuel Iyer'],
    directorId: 'p14',
  }),
  movie({
    id: 'mv26',
    title: 'The Long Approach',
    year: 2021,
    genres: ['drama', 'adventure'],
    certification: 'PG',
    synopsis:
      "A retired airline captain teaches his estranged daughter to fly the same route he flew the night she was born, over the course of one very overdue summer.",
    rating: 7.9,
    runtimeMinutes: 111,
    castIds: ['p02', 'p23'],
    castRoles: ['Theo Okafor', 'Talia Brandt'],
    directorId: 'p18',
  }),
  movie({
    id: 'mv27',
    title: 'Gravity Well',
    year: 2023,
    genres: ['sci-fi', 'thriller'],
    certification: '15',
    synopsis:
      "A station engineer has fourteen minutes of comms window per orbit to convince ground control that the failure isn’t hers before they cut the station loose.",
    rating: 8.0,
    runtimeMinutes: 113,
    castIds: ['p15', 'p19'],
    castRoles: ['Yuki Sato', 'Ivy Castellan'],
    directorId: 'p06',
  }),
  movie({
    id: 'mv28',
    title: 'The Orchard Keeps',
    year: 2020,
    genres: ['drama', 'romance'],
    certification: '12',
    synopsis:
      'Two families who have shared a property line and a decades-old feud for three generations find their kids running the same orchard together, whether they like it or not.',
    rating: 7.2,
    runtimeMinutes: 100,
    castIds: ['p09', 'p17'],
    castRoles: ['Freya Lindqvist', 'Callum Reyes'],
    directorId: 'p01',
  }),
];

function generateEpisodes(
  titleId: string,
  seasonNumber: number,
  count: number,
  premises: { title: string; synopsis: string }[],
): Episode[] {
  return premises
    .slice(0, count)
    .map((p, i) => ep(titleId, seasonNumber, i + 1, p.title, p.synopsis, 42 + ((i * 3) % 12)));
}

export const SERIES: TitleRecord[] = [
  series({
    id: 'sr01',
    title: 'The Ferryman’s Ledger',
    year: 2024,
    genres: ['drama', 'crime'],
    certification: '15',
    synopsis:
      'A harbor customs officer inherits her uncle\'s unofficial ledger of favors owed across the port, and has one season to decide which debts to honor before it gets her killed.',
    rating: 8.4,
    seasonCount: 1,
    progress: 0.35,
    resumeEpisode: { seasonNumber: 1, episodeNumber: 4, title: 'Low Tide' },
    castIds: ['p01', 'p12', 'p24'],
    castRoles: ['Margot Lin', 'Owen Blackwood', 'Noor Haddad'],
    creatorIds: ['p09'],
    seasons: [
      season(1, 'Season 1', [
        ep('sr01', 1, 1, 'The Ledger', 'Margot inherits a locked drawer and a port full of people who already know what’s in it.', 48),
        ep('sr01', 1, 2, 'Collections', 'The first debtor on the list isn’t who Margot expected — and he isn’t willing to pay.', 45),
        ep('sr01', 1, 3, 'The Harbor Master', 'A routine inspection turns into leverage Margot can’t afford to ignore.', 47),
        ep('sr01', 1, 4, 'Low Tide', 'A shipment goes missing on Margot’s watch, and the ledger says she already knew it would.', 46, 0.6),
        ep('sr01', 1, 5, 'Ballast', 'Margot calls in a favor of her own, and learns the ledger only works one way.', 44),
        ep('sr01', 1, 6, 'The Uncle’s Rule', 'The season finale: the debt that started it all comes due, and Margot has to write the next entry herself.', 51),
      ]),
    ],
  }),
  series({
    id: 'sr02',
    title: 'Northbound',
    year: 2023,
    genres: ['drama', 'adventure'],
    certification: '12',
    synopsis:
      "Four strangers on a delayed train through the northern provinces are rerouted by a landslide onto a route that hasn’t run in a decade, and onto each other's lives.",
    rating: 7.9,
    seasonCount: 2,
    castIds: ['p05', 'p20', 'p23'],
    castRoles: ['Priya Anand', 'Malik Preston', 'Talia Brandt'],
    creatorIds: ['p03'],
    seasons: [
      season(1, 'Season 1', generateEpisodes('sr02', 1, 6, [
        { title: 'The Landslide', synopsis: 'A washed-out line strands the 11:40 sleeper at a station that closed in 2014.' },
        { title: 'Platform Four', synopsis: 'The stationmaster who never left starts telling four strangers more than they asked for.' },
        { title: 'The Other Ticket', synopsis: 'Priya finds a boarding pass in her bag that isn’t hers, for a train that already left.' },
        { title: 'Freight', synopsis: 'A cargo car nobody remembers loading turns the delay into something else.' },
        { title: 'The Long Siding', synopsis: 'Old grievances surface while the replacement engine is still six hours out.' },
        { title: 'Northbound', synopsis: 'The line finally reopens, and not everyone is getting back on.' },
      ])),
      season(2, 'Season 2', generateEpisodes('sr02', 2, 6, [
        { title: 'A Year On', synopsis: 'The four strangers, now not strangers, take the same route on purpose this time.' },
        { title: 'The New Timetable', synopsis: 'A private buyer has plans for the line that don’t include a public stop.' },
        { title: 'Ballast and Sleepers', synopsis: 'Malik goes looking for the freight car from last season and finds out who it belonged to.' },
        { title: 'The Siding, Revisited', synopsis: 'An old grievance resurfaces at the worst possible junction.' },
        { title: 'Last Call', synopsis: 'The buyout vote is tomorrow, and Talia has the one document that could stop it.' },
        { title: 'Terminus', synopsis: 'The season closes at the end of the line, for good this time.' },
      ])),
    ],
  }),
  series({
    id: 'sr03',
    title: 'Halflight',
    year: 2025,
    genres: ['sci-fi', 'thriller'],
    certification: '15',
    synopsis:
      "In a research town that only gets four hours of usable daylight a year, a systems analyst discovers the observatory has been recording something in the dark for decades.",
    rating: 8.6,
    seasonCount: 1,
    progress: 0.12,
    resumeEpisode: { seasonNumber: 1, episodeNumber: 2, title: 'The Archive' },
    castIds: ['p19', 'p08', 'p15'],
    castRoles: ['Ivy Castellan', 'Hiro Tanaka', 'Yuki Sato'],
    creatorIds: ['p06'],
    seasons: [
      season(1, 'Season 1', [
        ep('sr03', 1, 1, 'Polar Night', 'Ivy transfers to the observatory expecting quiet. The first log entry she reads isn’t quiet at all.', 50),
        ep('sr03', 1, 2, 'The Archive', 'Forty years of recordings, and one frequency that shouldn’t exist appears in every single one.', 47, 0.4),
        ep('sr03', 1, 3, 'Four Hours', 'The town’s one day of sunlight arrives, and everyone acts like it’s a countdown.', 46),
        ep('sr03', 1, 4, 'Signal Discipline', 'Hiro breaks protocol to answer the frequency, and gets an answer back.', 49),
        ep('sr03', 1, 5, 'What the Ice Keeps', 'A core sample dated before the observatory existed changes everything Ivy thought she knew.', 48),
        ep('sr03', 1, 6, 'Halflight', 'The season finale: the four hours of daylight arrive again, and this time everyone is watching the dark instead.', 54),
      ]),
    ],
  }),
  series({
    id: 'sr04',
    title: 'The Understudies',
    year: 2022,
    genres: ['comedy', 'drama'],
    certification: '15',
    synopsis:
      'A regional theatre’s permanently broke ensemble cast tries to save the building by mounting a season nobody thinks they can pull off.',
    rating: 7.6,
    seasonCount: 1,
    castIds: ['p11', 'p25', 'p16', 'p02'],
    castRoles: ['Clara Duval', 'Miles Ashworth', 'Rosa Herrera', 'Theo Okafor'],
    creatorIds: ['p01'],
    seasons: [
      season(1, 'Season 1', generateEpisodes('sr04', 1, 8, [
        { title: 'Closing Notice', synopsis: 'The council gives the Regent Theatre one season to prove it still matters.' },
        { title: 'Casting Against Type', synopsis: 'Clara casts the accountant threatening to foreclose them as the lead.' },
        { title: 'Previews', synopsis: 'A disastrous first preview somehow sells out the second.' },
        { title: 'The Understudy’s Understudy', synopsis: 'Nobody can find Miles on opening night of the second show.' },
        { title: 'Backstage', synopsis: 'A feud twenty years old resurfaces in the wings.' },
        { title: 'The Reviews', synopsis: 'One critic’s notice could save the theatre — or finish it.' },
        { title: 'Fundraiser', synopsis: 'A gala goes sideways when the guest of honor turns out to be an old flame.' },
        { title: 'Curtain', synopsis: 'The season finale performance is also the vote on the building’s future.' },
      ])),
    ],
  }),
  series({
    id: 'sr05',
    title: 'Redshift',
    year: 2021,
    genres: ['sci-fi', 'drama'],
    certification: '12',
    synopsis:
      "The crew of a deep-space survey vessel ages at a different rate than the family they left behind, told across the mission and the years back home in parallel.",
    rating: 8.1,
    seasonCount: 1,
    castIds: ['p10', 'p13', 'p22'],
    castRoles: ['Samuel Iyer', 'Amara Nwosu', 'Adrian Voss'],
    creatorIds: ['p08'],
    seasons: [
      season(1, 'Season 1', generateEpisodes('sr05', 1, 7, [
        { title: 'Departure', synopsis: 'Samuel leaves for a six-month survey that will take eleven years to complete on the ground.' },
        { title: 'Six Months In', synopsis: 'The crew adjusts to a mission clock nobody at home can quite picture.' },
        { title: 'Three Years On the Ground', synopsis: 'Amara raises a daughter alone and starts recording messages the mission may never receive in time.' },
        { title: 'Drift', synopsis: 'A course correction costs the crew eight more months they didn’t plan for.' },
        { title: 'The Message Backlog', synopsis: 'A decade of unopened messages arrive at once, and Samuel has to choose which to open first.' },
        { title: 'Turnaround', synopsis: 'The survey completes, and the crew realizes going home means arriving in a future none of them chose.' },
        { title: 'Redshift', synopsis: 'The season finale: touchdown, eleven years late, to a family that kept living.' },
      ])),
    ],
  }),
  series({
    id: 'sr06',
    title: 'Blackwood & Iyer',
    year: 2020,
    genres: ['crime', 'drama'],
    certification: '15',
    synopsis:
      'A disbarred defense attorney and a suspended detective run an unlicensed investigations office out of a laundromat back room, taking the cases the system already gave up on.',
    rating: 7.7,
    seasonCount: 2,
    castIds: ['p12', 'p10'],
    castRoles: ['Owen Blackwood', 'Samuel Iyer'],
    creatorIds: ['p14'],
    seasons: [
      season(1, 'Season 1', generateEpisodes('sr06', 1, 6, [
        { title: 'Off the Books', synopsis: 'Owen and Samuel take a case nobody with a badge or a bar card is allowed to touch.' },
        { title: 'The Laundromat', synopsis: 'Their new office comes with a landlord who already knows too much about them.' },
        { title: 'Chain of Custody', synopsis: 'Evidence that could clear their client goes missing from an evidence locker that was never opened.' },
        { title: 'Old Colleagues', synopsis: 'Samuel’s former partner offers help that comes with strings neither of them can see yet.' },
        { title: 'The Retainer', synopsis: 'A client with real money wants to hire them permanently — for the wrong reasons.' },
        { title: 'Verdict', synopsis: 'The season finale case comes down to a witness only Owen can find in time.' },
      ])),
      season(2, 'Season 2', generateEpisodes('sr06', 2, 6, [
        { title: 'Reinstated', synopsis: 'Samuel’s suspension is lifted, and it changes everything about how the office runs.' },
        { title: 'Two Licenses', synopsis: 'A case forces Owen and Samuel to work opposite sides of the same courtroom.' },
        { title: 'The Landlord’s Favor', synopsis: 'Their landlord finally calls in what she’s owed.' },
        { title: 'Discovery', synopsis: 'A box of files in the back room turns out to belong to the case that got Owen disbarred.' },
        { title: 'Cross', synopsis: 'Samuel takes the stand against his own former department.' },
        { title: 'Closing Argument', synopsis: 'The season finale: the case that could get Owen his bar card back, or cost them the office.' },
      ])),
    ],
  }),
  series({
    id: 'sr07',
    title: 'Greenhouse',
    year: 2024,
    genres: ['drama', 'romance'],
    certification: '12',
    synopsis:
      "A struggling botanical garden hires a disgraced former head gardener to save its centerpiece greenhouse, one season and one very reluctant staff at a time.",
    rating: 7.5,
    seasonCount: 1,
    castIds: ['p09', 'p17', 'p05'],
    castRoles: ['Freya Lindqvist', 'Callum Reyes', 'Priya Anand'],
    creatorIds: ['p03'],
    seasons: [
      season(1, 'Season 1', generateEpisodes('sr07', 1, 6, [
        { title: 'The Interview', synopsis: 'Freya takes a job the board only offered because nobody else applied.' },
        { title: 'Root Bound', synopsis: 'The greenhouse’s prize collection is dying, and the reason is buried in old paperwork.' },
        { title: 'Cuttings', synopsis: 'Freya and Callum clash over a restoration method that could save the collection or kill it faster.' },
        { title: 'The Board Meeting', synopsis: 'A vote to sell the greenhouse’s land forces everyone to make their case.' },
        { title: 'Bloom', synopsis: 'The collection flowers for the first time in years, at the worst possible moment.' },
        { title: 'Open Day', synopsis: 'The season finale: a public open day the greenhouse needs to succeed to survive.' },
      ])),
    ],
  }),
  series({
    id: 'sr08',
    title: 'The Salvage Crew',
    year: 2023,
    genres: ['sci-fi', 'adventure'],
    certification: '12',
    synopsis:
      "A four-person salvage ship works the debris field of a decade-old orbital war, pulling scrap and secrets in equal measure from wrecks nobody else will touch.",
    rating: 7.8,
    seasonCount: 1,
    castIds: ['p06', 'p19', 'p24', 'p17'],
    castRoles: ['Diego Fuentes', 'Ivy Castellan', 'Noor Haddad', 'Callum Reyes'],
    creatorIds: ['p08'],
    seasons: [
      season(1, 'Season 1', generateEpisodes('sr08', 1, 8, [
        { title: 'Debris Field', synopsis: 'The crew stakes a claim on a wreck bigger than anything they’ve pulled before.' },
        { title: 'Salvage Rights', synopsis: 'A rival crew contests the claim, and the paperwork trail goes nowhere good.' },
        { title: 'Cargo Manifest', synopsis: 'What’s actually in the wreck’s hold was never meant to survive the war.' },
        { title: 'Dead Reckoning', synopsis: 'Navigation fails deep inside the wreck, and Noor has to get the crew out by memory.' },
        { title: 'The Buyer', synopsis: 'A buyer for the cargo turns out to have fought on the losing side of the war it came from.' },
        { title: 'Pressure Hull', synopsis: 'A slow leak turns a routine haul into a survival run.' },
        { title: 'Old Debts', synopsis: 'Diego’s history with the rival crew’s captain finally comes out.' },
        { title: 'Salvage Value', synopsis: 'The season finale: the crew decides what the cargo is really worth, and to whom.' },
      ])),
    ],
  }),
  series({
    id: 'sr09',
    title: 'Firebreak',
    year: 2022,
    genres: ['action', 'drama'],
    certification: '15',
    synopsis:
      'A wildland fire crew based out of a shrinking mountain station spends one brutal season holding lines nobody upstream is willing to fund properly.',
    rating: 7.9,
    seasonCount: 1,
    castIds: ['p07', 'p04', 'p20'],
    castRoles: ['Naomi Cross', 'Marcus Webb', 'Malik Preston'],
    creatorIds: ['p04'],
    seasons: [
      season(1, 'Season 1', generateEpisodes('sr09', 1, 6, [
        { title: 'First Call', synopsis: 'A dry spring puts the crew on the line six weeks earlier than budgeted.' },
        { title: 'The Line', synopsis: 'A firebreak cut too close to a ranch forces an impossible call.' },
        { title: 'Spot Fire', synopsis: 'Embers cross the break, and the crew has to outrun their own plan.' },
        { title: 'Mutual Aid', synopsis: 'A neighboring district’s crew arrives short-staffed and under-equipped.' },
        { title: 'The Budget Hearing', synopsis: 'Naomi testifies in the city while her crew works a blaze without her.' },
        { title: 'Containment', synopsis: 'The season finale: the fire that started it all reaches the station itself.' },
      ])),
    ],
  }),
  series({
    id: 'sr10',
    title: 'Paper Trail',
    year: 2019,
    genres: ['comedy', 'crime'],
    certification: '15',
    synopsis:
      'A hapless municipal records clerk accidentally uncovers a decades-long fraud in the archive he’s about to digitize, and can’t convince anyone powerful enough to care.',
    rating: 7.0,
    seasonCount: 1,
    castIds: ['p25', 'p16'],
    castRoles: ['Miles Ashworth', 'Rosa Herrera'],
    creatorIds: ['p13'],
    seasons: [
      season(1, 'Season 1', generateEpisodes('sr10', 1, 6, [
        { title: 'Box 4471', synopsis: 'Miles finds a filing error that’s actually been hiding forty years of fraud.' },
        { title: 'The Digitization Deadline', synopsis: 'The archive gets shredded in six weeks unless Miles can prove what he found first.' },
        { title: 'Nobody Believes the Clerk', synopsis: 'Every office Miles reports to has a reason not to look.' },
        { title: 'Rosa', synopsis: 'A city auditor takes Miles seriously for entirely her own reasons.' },
        { title: 'The Shredding Truck', synopsis: 'A race against the literal truck scheduled to destroy the evidence.' },
        { title: 'Filed', synopsis: 'The season finale: getting the truth on the record before the record disappears.' },
      ])),
    ],
  }),
];

export const ALL_TITLES: TitleRecord[] = [...MOVIES, ...SERIES];

export function titleById(id: string): TitleRecord | undefined {
  return ALL_TITLES.find((t) => t.id === id);
}

export function resolveCast(t: TitleRecord) {
  return castFor(t.castIds, t.castRoles);
}

export function resolveDirector(t: TitleRecord) {
  return t.directorId ? personById(t.directorId) : undefined;
}

export function resolveCreators(t: TitleRecord) {
  return t.creatorIds?.map((id) => personById(id));
}
