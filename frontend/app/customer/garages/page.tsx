'use client';

import { useState } from 'react';
import {
  PageHeader,
  Card,
  CardHeader,
  CardTitle,
  CardContent,
  Badge,
  EmptyState,
  Button,
} from '@/lib/components/ui';
import styles from './garages.module.css';

type GarageRegion = 'North' | 'South' | 'Central';

interface DemoGarage {
  id: string;
  name: string;
  region: GarageRegion;
  city: string;
  isPanelGarage: boolean;
  acceptingWork: boolean;
}

const demoGarages: DemoGarage[] = [
  {
    id: 'north-jaffna-1',
    name: 'Jaffna Motor Works',
    region: 'North',
    city: 'Jaffna',
    isPanelGarage: true,
    acceptingWork: true,
  },
  {
    id: 'north-jaffna-2',
    name: 'Northern Auto Care',
    region: 'North',
    city: 'Jaffna',
    isPanelGarage: true,
    acceptingWork: true,
  },
  {
    id: 'north-jaffna-3',
    name: 'Peninsula Vehicle Centre',
    region: 'North',
    city: 'Jaffna',
    isPanelGarage: false,
    acceptingWork: true,
  },
  {
    id: 'north-jaffna-4',
    name: 'Jaffna Precision Repairs',
    region: 'North',
    city: 'Jaffna',
    isPanelGarage: true,
    acceptingWork: false,
  },
  {
    id: 'south-galle-1',
    name: 'Galle Coast Auto',
    region: 'South',
    city: 'Galle',
    isPanelGarage: true,
    acceptingWork: true,
  },
  {
    id: 'south-galle-2',
    name: 'Southern Motor Care',
    region: 'South',
    city: 'Galle',
    isPanelGarage: true,
    acceptingWork: true,
  },
  {
    id: 'south-galle-3',
    name: 'Harbour Road Vehicle Works',
    region: 'South',
    city: 'Galle',
    isPanelGarage: false,
    acceptingWork: true,
  },
  {
    id: 'south-galle-4',
    name: 'Galle Bay Auto Service',
    region: 'South',
    city: 'Galle',
    isPanelGarage: true,
    acceptingWork: false,
  },
  {
    id: 'central-colombo-1',
    name: 'Colombo City Motor Works',
    region: 'Central',
    city: 'Colombo',
    isPanelGarage: true,
    acceptingWork: true,
  },
  {
    id: 'central-kandy-1',
    name: 'Kandy Hill Country Auto',
    region: 'Central',
    city: 'Kandy',
    isPanelGarage: true,
    acceptingWork: true,
  },
  {
    id: 'central-negombo-1',
    name: 'Negombo Lagoon Vehicle Care',
    region: 'Central',
    city: 'Negombo',
    isPanelGarage: false,
    acceptingWork: true,
  },
  {
    id: 'central-kurunegala-1',
    name: 'Kurunegala Motor Centre',
    region: 'Central',
    city: 'Kurunegala',
    isPanelGarage: true,
    acceptingWork: false,
  },
];

const regions: GarageRegion[] = ['North', 'South', 'Central'];

export default function CustomerGaragesPage() {
  const [selectedRegion, setSelectedRegion] = useState<GarageRegion | null>(null);
  const [selectedCity, setSelectedCity] = useState('all');
  const [query, setQuery] = useState('');
  const [searchTerm, setSearchTerm] = useState('');

  const availableCities = selectedRegion
    ? Array.from(
        new Set(
          demoGarages
            .filter((garage) => garage.region === selectedRegion)
            .map((garage) => garage.city),
        ),
      )
    : [];

  const filteredGarages = selectedRegion
    ? demoGarages.filter((garage) => {
        const matchesRegion = garage.region === selectedRegion;
        const matchesCity = selectedCity === 'all' || garage.city === selectedCity;
        const searchText = `${garage.name} ${garage.city}`.toLowerCase();
        const matchesSearch = searchText.includes(searchTerm.trim().toLowerCase());

        return matchesRegion && matchesCity && matchesSearch;
      })
    : [];

  const selectRegion = (region: GarageRegion) => {
    setSelectedRegion(region);
    setSelectedCity('all');
  };

  return (
    <div className={styles.page}>
      <PageHeader
        title="Panel Garages"
        description="Choose a region to explore repair options near you."
      />

      <aside className={styles.demoNotice} aria-label="Sample data notice">
        <span className={styles.demoNoticeMark} aria-hidden="true">i</span>
        <p>
          <strong>Temporary demo data</strong>
          {' '}— these fictional sample listings are for preview only. They are not
          verified garages, and no real contact details or navigable addresses are provided.
        </p>
      </aside>

      <section className={styles.regionSection} aria-labelledby="region-heading">
        <div className={styles.sectionIntro}>
          <p className={styles.eyebrow}>Step 1</p>
          <h2 id="region-heading">Select a region</h2>
          <p>Start with the area where you would like your vehicle repaired.</p>
        </div>

        <div className={styles.regionChoices} role="group" aria-label="Garage regions">
          {regions.map((region) => {
            const cityCount = new Set(
              demoGarages
                .filter((garage) => garage.region === region)
                .map((garage) => garage.city),
            ).size;

            return (
              <button
                key={region}
                type="button"
                className={`${styles.regionChoice} ${
                  selectedRegion === region ? styles.regionChoiceSelected : ''
                }`}
                aria-pressed={selectedRegion === region}
                onClick={() => selectRegion(region)}
              >
                <span className={styles.regionMarker} aria-hidden="true">
                  <svg viewBox="0 0 24 24" fill="none">
                    <path d="M12 21s7-6.1 7-12a7 7 0 1 0-14 0c0 5.9 7 12 7 12Z" />
                    <circle cx="12" cy="9" r="2.4" />
                  </svg>
                </span>
                <span className={styles.regionName}>{region}</span>
                <span className={styles.regionMeta}>
                  {cityCount} {cityCount === 1 ? 'city' : 'cities'} in this demo
                </span>
                <span className={styles.regionArrow} aria-hidden="true">→</span>
              </button>
            );
          })}
        </div>
      </section>

      {selectedRegion ? (
        <section className={styles.resultsSection} aria-labelledby="results-heading">
          <div className={styles.resultsHeading}>
            <div className={styles.sectionIntro}>
              <p className={styles.eyebrow}>Step 2 · {selectedRegion} region</p>
              <h2 id="results-heading">Find a sample garage</h2>
              <p>Filter by city or search by garage name. Both filters work together.</p>
            </div>
            <p className={styles.resultCount} aria-live="polite">
              <strong>{filteredGarages.length}</strong>
              {' '}{filteredGarages.length === 1 ? 'sample listing' : 'sample listings'}
            </p>
          </div>

          <div className={styles.filters}>
            <div className={styles.cityFilter}>
              <label htmlFor="garage-city">City</label>
              <select
                id="garage-city"
                className={styles.select}
                value={selectedCity}
                onChange={(event) => setSelectedCity(event.target.value)}
              >
                <option value="all">All cities in {selectedRegion}</option>
                {availableCities.map((city) => (
                  <option key={city} value={city}>{city}</option>
                ))}
              </select>
            </div>

            <form
              className={styles.searchRow}
              role="search"
              onSubmit={(event) => {
                event.preventDefault();
                setSearchTerm(query);
              }}
            >
              <label className={styles.searchLabel} htmlFor="garage-search">
                Search garages
              </label>
              <input
                id="garage-search"
                className={styles.searchInput}
                type="search"
                value={query}
                onChange={(event) => setQuery(event.target.value)}
                placeholder="Search by garage name or city"
              />
              <Button type="submit">Search</Button>
            </form>
          </div>

          {filteredGarages.length === 0 ? (
            <EmptyState
              title="No sample garages found"
              description="Try another city or a different search term."
            />
          ) : (
            <div className={styles.grid}>
              {filteredGarages.map((garage) => (
                <Card key={garage.id} className={styles.garageCard}>
                  <CardHeader className={styles.garageHeader}>
                    <div>
                      <div className={styles.cardEyebrow}>
                        <span className={styles.demoTag}>Demo listing</span>
                        <span>{garage.region} region</span>
                      </div>
                      <CardTitle>{garage.name}</CardTitle>
                      <p className={styles.city}>{garage.city}</p>
                    </div>
                  </CardHeader>
                  <CardContent>
                    <div className={styles.contactPlaceholder}>
                      <p>Temporary sample location</p>
                      <span>Not a real address · Not for navigation</span>
                    </div>
                    <p className={styles.contactPlaceholder}>
                      Contact details are not provided for demo listings.
                    </p>
                    <div className={styles.foot}>
                      {garage.isPanelGarage && (
                        <Badge variant="info" size="sm">Panel</Badge>
                      )}
                      <Badge
                        variant={garage.acceptingWork ? 'success' : 'secondary'}
                        size="sm"
                      >
                        {garage.acceptingWork ? 'Accepting work' : 'Not accepting'}
                      </Badge>
                    </div>
                  </CardContent>
                </Card>
              ))}
            </div>
          )}
        </section>
      ) : (
        <div className={styles.selectionPrompt} role="status">
          <span className={styles.promptStep}>01</span>
          <div>
            <h2>Your search starts with an area</h2>
            <p>Select one of the regions above to see its cities and demo listings.</p>
          </div>
        </div>
      )}
    </div>
  );
}
